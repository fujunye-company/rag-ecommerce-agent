"""Product schemas"""
from pydantic import BaseModel
from uuid import UUID


class ProductBase(BaseModel):
    title: str
    description: str | None = None
    price: float
    category: str
    brand: str | None = None


class ProductCreate(ProductBase):
    pass


class ProductRead(ProductBase):
    id: UUID
    rating: float = 0
    image_urls: list[str] = []

    model_config = {"from_attributes": True}
