from pydantic import BaseModel, ConfigDict


class ErrorResponse(BaseModel):
    detail: str


class HealthResponse(BaseModel):
    status: str
    database: str


class ORMModel(BaseModel):
    model_config = ConfigDict(from_attributes=True, extra="ignore")
