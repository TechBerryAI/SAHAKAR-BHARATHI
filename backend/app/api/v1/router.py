from fastapi import APIRouter

from app.api.v1 import admin, attendance, me, phone_verifications, places, registrations, sessions

api_router = APIRouter()
api_router.include_router(admin.router)
api_router.include_router(places.router)
api_router.include_router(registrations.router)
api_router.include_router(phone_verifications.router)
api_router.include_router(sessions.router)
api_router.include_router(me.router)
api_router.include_router(attendance.router)
