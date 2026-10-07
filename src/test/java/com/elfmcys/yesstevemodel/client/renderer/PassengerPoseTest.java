package com.elfmcys.yesstevemodel.client.renderer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PassengerPoseTest {
    @Test
    void correctionIsIndependentOfWorldAltitudeAndIncludesRiderAttachment() {
        for (double vehicleY : new double[]{-64.0, 0.0, 64.0, 128.0, 300.0}) {
            double seatWorldY = vehicleY + 1.5;
            double riderAttachmentY = 0.25;
            double correction = CustomVehicleRenderer.passengerVerticalCorrection(vehicleY, seatWorldY, riderAttachmentY);
            assertEquals(-1.25, correction, 1e-9);
            // 26.3 positionRider 的实际实体 Y 为 seatWorldY - riderAttachmentY。
            assertEquals(vehicleY, seatWorldY - riderAttachmentY + correction, 1e-9);
        }
    }
}
