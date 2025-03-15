package com.morichal.demo.services;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fazecast.jSerialComm.SerialPort;

@Service
public class WeightService {

private static final Logger logger = LoggerFactory.getLogger(WeightService.class);
    private final SerialPort serialPort;

    public WeightService() {
        // Configura el puerto serie
        serialPort = SerialPort.getCommPort("/dev/ttyUSB0"); // Cambia esto según tu configuración
        serialPort.setComPortParameters(9600, 8, 1, 0);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
        serialPort.openPort();
    }

    public Optional<Double> getWeight() {
        if (serialPort.isOpen()) {
            byte[] readBuffer = new byte[1024];
            int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
            String weightData = new String(readBuffer, 0, numRead).trim();
            try {
                double weight = Double.parseDouble(weightData);
                if (weight >= 0) { // Validación adicional para asegurar que el peso es válido
                    return Optional.of(weight);
                } else {
                    logger.warn("Peso inválido leído: {}", weight);
                }
            } catch (NumberFormatException e) {
                logger.error("Error al parsear el peso: {}", weightData, e);
            }
} else {
            logger.warn("El puerto serie no está abierto.");
        }
        return Optional.empty();
    }

    public void closePort() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }
}