from fastapi import HTTPException, Security, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt, JWTError
from typing import Optional
from uuid import UUID

from app.config import get_settings

security = HTTPBearer()


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
            raise HTTPException(status_code=401, detail="Invalid token: missing user ID")
        
        return UserPrincipal(
            user_id=UUID(user_id),
            profile_id=UUID(profile_id) if profile_id else None,
            email=email or "",
            token=token
        )
        
    except JWTError as e:
        raise HTTPException(status_code=401, detail=f"Invalid token: {str(e)}")


def require_profile(user: UserPrincipal = Depends(get_current_user)) -> UserPrincipal:
    """Ensure user has a profile ID."""
    if not user.profile_id:
        raise HTTPException(
            status_code=400,
            detail="Profile not found. Please complete your profile setup."
        )
    return user

