from __future__ import annotations

import time
from typing import Protocol

import httpx

from app.core.errors import SmsDeliveryError

_SENT = {"sent", "delivered"}
_FAILED = {"failed"}


class SmsGateway(Protocol):
    def send(self, recipient_e164: str, message: str) -> None: ...

    def wait_until_sent(self, timeout: float = 20.0) -> bool: ...


class TextbeeGateway:
    """Sends SMS through the Textbee gateway (https://textbee.dev)."""

    def __init__(
        self,
        api_key: str,
        device_id: str = "",
        base_url: str = "https://api.textbee.dev/api/v1",
        timeout: float = 15.0,
        transport: httpx.BaseTransport | None = None,
        sim_subscription_id: int | None = None,
        resolve_sim: bool = False,
    ) -> None:
        self._api_key = api_key
        self._device_id = device_id.strip()
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout
        self._transport = transport
        self._sim_subscription_id = sim_subscription_id
        self._resolve_sim = resolve_sim
        self._last_batch_id: str | None = None

    def send(self, recipient_e164: str, message: str) -> None:
        body: dict[str, object] = {
            "recipients": [recipient_e164],
            "message": message,
        }
        if self._device_id:
            body["deviceId"] = self._device_id
        try:
            with httpx.Client(timeout=self._timeout, transport=self._transport) as client:
                sim_id = self._subscription_id(client)
                if sim_id is not None:
                    body["simSubscriptionId"] = sim_id
                response = client.post(
                    f"{self._base_url}/gateway/send-sms",
                    headers={"x-api-key": self._api_key},
                    json=body,
                )
        except httpx.HTTPError as exc:
            raise SmsDeliveryError("Could not reach the SMS gateway.") from exc
        if response.status_code == 429:
            raise SmsDeliveryError("The SMS gateway is busy. Try again later.")
        if response.status_code != 200 or not _accepted(response):
            raise SmsDeliveryError()
        self._last_batch_id = _batch_id(response)

    def wait_until_sent(self, timeout: float = 20.0) -> bool:
        if not self._last_batch_id:
            return False
        deadline = time.monotonic() + timeout
        try:
            with httpx.Client(timeout=self._timeout, transport=self._transport) as client:
                while True:
                    status = self._batch_status(client)
                    if status in _SENT:
                        return True
                    if status in _FAILED:
                        return False
                    if time.monotonic() >= deadline:
                        return False
                    time.sleep(2.0)
        except httpx.HTTPError:
            return False

    def _batch_status(self, client: httpx.Client) -> str | None:
        params: dict[str, str | int] = {"limit": 1}
        if self._last_batch_id:
            params["smsBatchId"] = self._last_batch_id
        elif self._device_id:
            params["deviceId"] = self._device_id
        else:
            return None
        response = client.get(
            f"{self._base_url}/gateway/messages",
            headers={"x-api-key": self._api_key},
            params=params,
        )
        if response.status_code != 200:
            return None
        try:
            payload = response.json()
        except ValueError:
            return None
        rows = payload.get("data") if isinstance(payload, dict) else None
        if isinstance(rows, dict):
            rows = rows.get("data") or [rows]
        if not isinstance(rows, list) or not rows:
            return None
        row = rows[0]
        if not isinstance(row, dict):
            return None
        status = row.get("status")
        return status.lower() if isinstance(status, str) else None

    def _subscription_id(self, client: httpx.Client) -> int | None:
        if self._sim_subscription_id is not None:
            return self._sim_subscription_id
        if not self._resolve_sim or not self._device_id:
            return None
        try:
            response = client.get(
                f"{self._base_url}/gateway/devices/{self._device_id}",
                headers={"x-api-key": self._api_key},
            )
        except httpx.HTTPError:
            return None
        if response.status_code != 200:
            return None
        return _in_service_subscription_id(response.json())


def _in_service_subscription_id(payload: object) -> int | None:
    """Pick the SIM that currently has service. A dual-SIM phone can also
    hold a second SIM with no service; sending on that SIM fails in the radio.
    """
    data = payload.get("data") if isinstance(payload, dict) else None
    device = data if isinstance(data, dict) else payload
    if not isinstance(device, dict):
        return None
    sim_info = device.get("simInfo")
    sims = sim_info.get("sims") if isinstance(sim_info, dict) else None
    if not isinstance(sims, list):
        return None
    ranked: list[tuple[int, int]] = []
    for sim in sims:
        if not isinstance(sim, dict):
            continue
        if sim.get("serviceState") != "IN_SERVICE" or sim.get("simState") != "READY":
            continue
        subscription_id = sim.get("subscriptionId")
        if not isinstance(subscription_id, int):
            continue
        signal = sim.get("signalLevel")
        ranked.append((signal if isinstance(signal, int) else 0, subscription_id))
    if not ranked:
        return None
    ranked.sort(reverse=True)
    return ranked[0][1]


def _batch_id(response: httpx.Response) -> str | None:
    try:
        payload = response.json()
    except ValueError:
        return None
    data = payload.get("data") if isinstance(payload, dict) else None
    if not isinstance(data, dict):
        return None
    batch_id = data.get("smsBatchId") or data.get("_id")
    return batch_id if isinstance(batch_id, str) and batch_id else None


def _accepted(response: httpx.Response) -> bool:
    try:
        payload = response.json()
    except ValueError:
        return False
    data = payload.get("data") if isinstance(payload, dict) else None
    if not isinstance(data, dict):
        return False
    if data.get("success") is True:
        return True
    success_count = int(data.get("successCount") or 0)
    failure_count = int(data.get("failureCount") or 0)
    return success_count > 0 and failure_count == 0
