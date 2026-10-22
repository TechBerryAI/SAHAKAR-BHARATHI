from fastapi import HTTPException, status


class AppError(HTTPException):
    def __init__(self, status_code: int, detail: str) -> None:
        super().__init__(status_code=status_code, detail=detail)


class DuplicateAccountError(AppError):
    def __init__(self) -> None:
        super().__init__(status.HTTP_409_CONFLICT, "An account with these details already exists.")


class InvalidCredentialsError(AppError):
    def __init__(self) -> None:
        super().__init__(status.HTTP_401_UNAUTHORIZED, "Login ID or PIN is incorrect.")


class RateLimitedError(AppError):
    def __init__(self) -> None:
        super().__init__(status.HTTP_429_TOO_MANY_REQUESTS, "Too many attempts. Try again later.")


class UnauthorizedError(AppError):
    def __init__(self, detail: str = "Authentication required.") -> None:
        super().__init__(status.HTTP_401_UNAUTHORIZED, detail)


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
