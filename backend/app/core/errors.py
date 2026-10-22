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


class InvalidCredentialsError(AppError):
    def __init__(self) -> None:
        super().__init__(
            status.HTTP_401_UNAUTHORIZED,
            "Login ID or PIN is incorrect.",
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


class UnauthorizedError(AppError):
    def __init__(self, detail: str = "Authentication required.") -> None:
        super().__init__(
            status.HTTP_401_UNAUTHORIZED,
            detail,
            headers={"WWW-Authenticate": "Bearer"},
        )
