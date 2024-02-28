package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.IImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    @Mock
    private IImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private StatusListener statusListener;

    Sensor sensor;
    Sensor sensor2;

    SecurityService securityService;


    @BeforeEach
    void init() {
        sensor = new Sensor("Sensor Door", SensorType.DOOR);
        sensor2 = new Sensor("Sensor Door 2", SensorType.DOOR);
        securityService = new SecurityService(securityRepository, imageService);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void AlarmIsArmed_SensorBecomesActivated_AlarmPending(ArmingStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(status);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void AlarmIsArmed_SensorBecomesActivatedSystemAlreadyPending_AlarmActive(ArmingStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(status);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void AlarmPending_SensorInactive_NoAlarm(ArmingStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(status).thenReturn(status);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM).thenReturn(AlarmStatus.PENDING_ALARM);

        // Preconditions
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);

        // Test
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void AlarmActive_SensorStateChanged_NoEffectAlarmState(boolean status) {
        if(status) {
            when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        }
        securityService.changeSensorActivationStatus(sensor, status);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    public void SensorActivatedTwice_AlarmPendingState_AlarmStatus() {
        sensor.setActive(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void SensorDeactivatedTwice_AlarmStateNotChanged() {
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    public void CatShown_SystemArmedHome_SetStatusAlarm() {
        BufferedImage bufferedImage = new BufferedImage(1, 1, 1);
        when(imageService.imageContainsCat(any(BufferedImage.class), any(float.class))).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(bufferedImage);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

        // Test
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.ARMED_HOME);
    }

    @Test
    public void CatShown_SystemDisarmed_SetStatusNoAlarm() {
        BufferedImage bufferedImage = new BufferedImage(1, 1, 1);
        ConcurrentSkipListSet<Sensor> sensors = new ConcurrentSkipListSet<>();
        sensors.add(sensor);
        when(imageService.imageContainsCat(any(BufferedImage.class), any(float.class))).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.processImage(bufferedImage);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void SystemDisarmed_SetStatusNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void SystemArmed_ResetAllSensors(ArmingStatus status) {
        sensor.setActive(true);
        sensor2.setActive(true);
        ConcurrentSkipListSet<Sensor> sensors = new ConcurrentSkipListSet<>();
        sensors.add(sensor);
        sensors.add(sensor2);

        // Test
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(status);

        securityService.getSensors().forEach(sensor -> {
            assertFalse(sensor.getActive());
        });
        verify(securityRepository, times(1)).setArmingStatus(status);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void SystemArmedHome_CatShown_SetStatusAlarm() {
        BufferedImage bufferedImage = new BufferedImage(1, 1, 1);
        when(imageService.imageContainsCat(any(BufferedImage.class), any(float.class))).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        securityService.processImage(bufferedImage);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    public void AddAndRemoveSensor ()
    {
        securityService.addSensor(sensor);
        verify(securityRepository, times(1)).addSensor(sensor);

        securityService.removeSensor(sensor);
        verify(securityRepository, times(1)).removeSensor(sensor);
    }

    @Test
    public void AddAndRemoveStatusListener ()
    {
        // Add listener expect it will be called
        securityService.addStatusListener(statusListener);
        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(statusListener, times(1)).notify(AlarmStatus.NO_ALARM);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

        // Remove listener expect that it count stays at one
        securityService.removeStatusListener(statusListener);
        securityService.setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(statusListener, times(1)).notify(AlarmStatus.NO_ALARM);
        verify(securityRepository, times(2)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void CatShown_SystemDisarmedSensorActive_NotChangeStatus() {
        BufferedImage bufferedImage = new BufferedImage(1, 1, 1);
        ConcurrentSkipListSet<Sensor> sensors = new ConcurrentSkipListSet<>();
        sensor.setActive(true);
        sensors.add(sensor);
        when(imageService.imageContainsCat(any(BufferedImage.class), any(float.class))).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.processImage(bufferedImage);

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
}
