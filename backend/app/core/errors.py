from fastapi import HTTPException, status


class AppError(HTTPException):
    def __init__(
        self,
        status_code: int,
        detail: str,
        headers: dict[str, str] | None = None,
    ) -> None:
        super().__init__(status_code=status_code, detail=detail, headers=headers)


class DuplicateAccountError(AppError):
    def __init__(self) -> None:
        super().__init__(status.HTTP_409_CONFLICT, "An account with these details already exists.")


class MobileAlreadyRegisteredError(AppError):
    def __init__(self) -> None:
        super().__init__(status.HTTP_409_CONFLICT, "An account already exists for this mobile number.")


class EmailAlreadyRegisteredError(AppError):
    def __init__(self) -> None:
        super().__init__(status.HTTP_409_CONFLICT, "An account already exists with this email address.")


class InvalidCredentialsError(AppError):
    def __init__(self) -> None:
        super().__init__(
            status.HTTP_401_UNAUTHORIZED,
            "Mobile number or PIN is incorrect.",
            headers={"WWW-Authenticate": "Bearer"},
        )


class RateLimitedError(AppError):
    def __init__(self, retry_after: int | None = None) -> None:
        headers = {"Retry-After": str(retry_after)} if retry_after is not None else None
        super().__init__(
            status.HTTP_429_TOO_MANY_REQUESTS,
            "Too many attempts. Try again later.",
            headers=headers,
        )


class ForbiddenError(AppError):
    def __init__(self, detail: str = "You do not have access to this.") -> None:
        super().__init__(status.HTTP_403_FORBIDDEN, detail)


class NotFoundError(AppError):
    def __init__(self, detail: str = "That person was not found.") -> None:
        super().__init__(status.HTTP_404_NOT_FOUND, detail)


class UnauthorizedError(AppError):
    def __init__(self, detail: str = "Authentication required.") -> None:
        super().__init__(
            status.HTTP_401_UNAUTHORIZED,
            detail,
            headers={"WWW-Authenticate": "Bearer"},
        )


class InvalidOtpError(AppError):
    def __init__(self) -> None:
        super().__init__(status.HTTP_400_BAD_REQUEST, "The code is incorrect or has expired.")


class VerificationRequiredError(AppError):
    def __init__(self) -> None:
        super().__init__(
            status.HTTP_400_BAD_REQUEST,
            "Verify this mobile number before registering.",
        )


class SmsDeliveryError(AppError):
    def __init__(self, detail: str = "Could not send the verification SMS. Try again.") -> None:
        super().__init__(status.HTTP_503_SERVICE_UNAVAILABLE, detail)
