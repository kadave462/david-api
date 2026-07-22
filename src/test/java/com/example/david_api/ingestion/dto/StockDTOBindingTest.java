package com.example.david_api.ingestion.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies StockDTO binds the snake_case JSON that SparkBind's Gson serializer
 * actually puts on the wire. Payload below is a real captured sample.
 */
class StockDTOBindingTest {

    private static final String SPARKBIND_WIRE_JSON = """
            {
              "hmac": "abc123",
              "stock_location_id": 1,
              "item_id": 15124,
              "item_name": "IBUPROFEN 400MG",
              "batch_number": "LOT-77",
              "expiration_date": "2026-07-22T12:26:55.396Z",
              "initial_quantity": 3000,
              "quantity": 2477,
              "unit_cost": 120.5,
              "unit_price": 250.0,
              "supplier_name": "MEDIPLUS LTD",
              "reference": "BL-9021",
              "item_code": "IBU400",
              "employee": "JEANNE",
              "id_lot": "L-15124-77"
            }
            """;

    @Test
    void bindsEverySnakeCaseFieldFromSparkBind() {
        StockDTO dto = JsonMapper.builder().build()
                .readValue(SPARKBIND_WIRE_JSON, StockDTO.class);

        // the four that were already working before the fix
        assertEquals("abc123", dto.getHmac());
        assertEquals(2477, dto.getQuantity());
        assertEquals("BL-9021", dto.getReference());
        assertEquals("JEANNE", dto.getEmployee());

        // the eleven that used to arrive as NULL
        assertEquals(1, dto.getStockLocationId());
        assertEquals(15124, dto.getItemId());
        assertEquals("IBUPROFEN 400MG", dto.getItemName());
        assertEquals("LOT-77", dto.getBatchNumber());
        assertEquals("2026-07-22T12:26:55.396Z", dto.getExpirationDate());
        assertEquals(3000, dto.getInitialQuantity());
        assertEquals(120.5, dto.getUnitCost());
        assertEquals(250.0, dto.getUnitPrice());
        assertEquals("MEDIPLUS LTD", dto.getSupplierName());
        assertEquals("IBU400", dto.getItemCode());
        assertEquals("L-15124-77", dto.getIdLot());

        assertNotNull(dto.getItemId(), "item_id must never bind to null again");
    }
}
