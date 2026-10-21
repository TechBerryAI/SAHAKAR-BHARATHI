from fastapi import APIRouter

from app.api.v1 import me, registrations, sessions

api_router = APIRouter()
api_router.include_router(registrations.router)
api_router.include_router(sessions.router)
api_router.include_router(me.router)
