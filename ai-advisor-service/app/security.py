from datetime import datetime, timezone
from fastapi import Security, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt, JWTError, ExpiredSignatureError
from typing import Optional
from uuid import UUID

from app.config import get_settings

security = HTTPBearer()


class ApiException(Exception):
    """Custom exception that carries structured error response."""

    def __init__(self, status_code: int, error_response: dict):
        self.status_code = status_code
        self.error_response = error_response
        super().__init__(error_response.get("message", "Error"))


class ApiErrorResponse:
    """Standardized error response matching Java services."""

    CODE_TOKEN_EXPIRED = "TOKEN_EXPIRED"
    CODE_TOKEN_INVALID = "TOKEN_INVALID"
    CODE_UNAUTHORIZED = "UNAUTHORIZED"
    CODE_FORBIDDEN = "FORBIDDEN"
    CODE_BAD_REQUEST = "BAD_REQUEST"
    CODE_NOT_FOUND = "NOT_FOUND"
    CODE_INTERNAL_ERROR = "INTERNAL_ERROR"

    @staticmethod
    def token_expired(path: str = "") -> dict:
        return {
            "code": ApiErrorResponse.CODE_TOKEN_EXPIRED,
            "status": 401,
            "message": "Your session has expired. Please log in again.",
            "path": path,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }

    @staticmethod
    def token_invalid(path: str = "", detail: str = None) -> dict:
        return {
            "code": ApiErrorResponse.CODE_TOKEN_INVALID,
            "status": 401,
            "message": "Invalid authentication token.",
            "detail": detail,
            "path": path,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }

    @staticmethod
    def unauthorized(path: str = "") -> dict:
        return {
            "code": ApiErrorResponse.CODE_UNAUTHORIZED,
            "status": 401,
            "message": "Authentication required. Please log in.",
            "path": path,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }

    @staticmethod
    def bad_request(path: str = "", detail: str = None) -> dict:
        return {
            "code": ApiErrorResponse.CODE_BAD_REQUEST,
            "status": 400,
            "message": "Bad request.",
            "detail": detail,
            "path": path,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }

    @staticmethod
    def internal_error(path: str = "") -> dict:
        return {
            "code": ApiErrorResponse.CODE_INTERNAL_ERROR,
            "status": 500,
            "message": "An unexpected error occurred. Please try again later.",
            "path": path,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }


class UserPrincipal:
    """Represents the authenticated user."""

    def __init__(self, user_id: UUID, profile_id: Optional[UUID], email: str, token: str):
        self.user_id = user_id
        self.profile_id = profile_id
        self.email = email
        self.token = token  # Original token for forwarding to other services


async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Security(security)
) -> UserPrincipal:
    """
    Validate JWT token and extract user information.
    Token format matches the Java services.
    """
    settings = get_settings()
    token = credentials.credentials

    try:
        # Accept both HS256 and HS384 algorithms for compatibility
        payload = jwt.decode(
            token,
            settings.jwt_secret,
            algorithms=["HS256", "HS384"],
            issuer=settings.jwt_issuer
        )

        user_id = payload.get("sub")
        profile_id = payload.get("profileId")
        email = payload.get("email")

        if not user_id:
            raise ApiException(401, ApiErrorResponse.token_invalid(detail="Missing user ID in token"))

        return UserPrincipal(
            user_id=UUID(user_id),
            profile_id=UUID(profile_id) if profile_id else None,
            email=email or "",
            token=token
        )

    except ExpiredSignatureError:
        raise ApiException(401, ApiErrorResponse.token_expired())
    except JWTError as e:
        error_msg = str(e)
        if "expired" in error_msg.lower():
            raise ApiException(401, ApiErrorResponse.token_expired())
        raise ApiException(401, ApiErrorResponse.token_invalid(detail=error_msg))


def require_profile(user: UserPrincipal = Depends(get_current_user)) -> UserPrincipal:
    """Ensure user has a profile ID."""
    if not user.profile_id:
        raise ApiException(400, ApiErrorResponse.bad_request(detail="Profile not found. Please complete your profile setup."))
    return user

