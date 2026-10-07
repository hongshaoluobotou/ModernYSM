package com.elfmcys.yesstevemodel.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class OuterFileTextureColorTest {
    @Test
    void bmpFallbackPreservesRedGreenAndBlueChannels() throws Exception {
        BufferedImage source = new BufferedImage(3, 1, BufferedImage.TYPE_INT_RGB);
        int[] colors = {0xffff0000, 0xff00ff00, 0xff0000ff};
        for (int x = 0; x < colors.length; x++) source.setRGB(x, 0, colors[x]);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(source, "bmp", bytes));
        Method readImage = OuterFileTexture.class.getDeclaredMethod("readImage", byte[].class);
        readImage.setAccessible(true);
        try (NativeImage decoded = (NativeImage) readImage.invoke(null, (Object) bytes.toByteArray())) {
            assertNotNull(decoded);
            for (int x = 0; x < colors.length; x++) assertEquals(colors[x], decoded.getPixel(x, 0));
        }
    }
}
