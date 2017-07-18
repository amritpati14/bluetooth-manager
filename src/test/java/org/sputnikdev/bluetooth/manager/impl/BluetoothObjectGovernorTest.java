package org.sputnikdev.bluetooth.manager.impl;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.sputnikdev.bluetooth.URL;
import org.sputnikdev.bluetooth.manager.BluetoothObjectType;
import org.sputnikdev.bluetooth.manager.BluetoothObjectVisitor;
import org.sputnikdev.bluetooth.manager.GovernorListener;
import org.sputnikdev.bluetooth.manager.NotReadyException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BluetoothObjectGovernorTest {

    private static final URL URL= new URL("/11:22:33:44:55:66");

    @Mock
    private BluetoothObject bluetoothObject;
    @Mock
    private BluetoothManagerImpl bluetoothManager;
    @Mock
    private GovernorListener governorListener;

    @InjectMocks
    @Spy
    private BluetoothObjectGovernor governor = new BluetoothObjectGovernor(bluetoothManager, URL) {
        @Override BluetoothObject findBluetoothObject() {
            return bluetoothObject;
        }

        @Override void reset(BluetoothObject object) {
        }

        @Override void update(BluetoothObject object) {
        }

        @Override void init(BluetoothObject object) {
        }

        @Override public BluetoothObjectType getType() {
            return BluetoothObjectType.ADAPTER;
        }

        @Override public void accept(BluetoothObjectVisitor visitor) throws Exception {
        }
    };

    @Before
    public void setUp() {

    }

    @Test
    public void testGetBluetoothObject() throws Exception {
        assertEquals(bluetoothObject, governor.getBluetoothObject());
        verify(governor, times(1)).getBluetoothObject();
        verifyNoMoreInteractions(governor);
    }

    @Test(expected = NotReadyException.class)
    public void testGetBluetoothObjectNotReady() throws Exception {
        Whitebox.setInternalState(governor, "bluetoothObject", null);
        governor.getBluetoothObject();
    }

    @Test
    public void testUpdateNotReady() throws Exception {
        Whitebox.setInternalState(governor, "bluetoothObject", null);
        when(governor.findBluetoothObject()).thenReturn(null);

        governor.update();

        // check interactions
        InOrder inOrder = inOrder(governor, governorListener);

        //inOrder.verify(governor, times(1)).update();
        inOrder.verify(governor, times(1)).findBluetoothObject();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testUpdateNotReadyToReady() throws Exception {
        // conditions
        Whitebox.setInternalState(governor, "bluetoothObject", null);
        when(governor.findBluetoothObject()).thenReturn(bluetoothObject);
        governor.addGovernorListener(governorListener);

        // invocation
        governor.update();

        // check interactions
        InOrder inOrder = inOrder(governor, governorListener);
        inOrder.verify(governor, times(1)).findBluetoothObject();
        inOrder.verify(governor, times(1)).init(bluetoothObject);
        inOrder.verify(governorListener, times(1)).ready(true);
        inOrder.verify(governor, times(1)).update(bluetoothObject);
        inOrder.verify(governorListener, times(1)).lastUpdatedChanged(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testUpdateReadyToNotReady() throws Exception {
        // conditions
        when(governor.findBluetoothObject()).thenReturn(bluetoothObject);
        doThrow(Exception.class).when(governor).update(bluetoothObject);
        governor.addGovernorListener(governorListener);

        // invocation
        governor.update();

        // check interactions
        InOrder inOrder = inOrder(governor, governorListener);
        inOrder.verify(governor, times(1)).update(bluetoothObject);
        inOrder.verify(governor, times(1)).reset(bluetoothObject);
        inOrder.verify(governorListener, times(1)).ready(false);

        inOrder.verifyNoMoreInteractions();
        assertNull(Whitebox.getInternalState(governor, "bluetoothObject"));
    }

    @Test
    public void testGetURL() throws Exception {
        assertEquals(URL, governor.getURL());
        verify(governor, times(1)).getURL();
        verifyNoMoreInteractions(governor);
    }

    @Test
    public void testAddGovernorListener() throws Exception {
        governor.addGovernorListener(governorListener);
        verify(governor, times(1)).addGovernorListener(governorListener);
        verifyNoMoreInteractions(governor);
    }

    @Test
    public void testRemoveGovernorListener() throws Exception {
        governor.removeGovernorListener(governorListener);
        verify(governor, times(1)).removeGovernorListener(governorListener);
        verifyNoMoreInteractions(governor);
    }

    @Test
    public void testLastChanged() throws Exception {
        governor.addGovernorListener(governorListener);

        Date lastChanged = governor.getLastActivity();

        governor.updateLastUpdated();

        InOrder inOrder = inOrder(governor, governorListener);
        assertTrue(lastChanged.before(governor.getLastActivity()));
        inOrder.verify(governor, times(1)).addGovernorListener(governorListener);
        inOrder.verify(governor, times(1)).getLastActivity();
        inOrder.verify(governor, times(1)).updateLastUpdated();
        inOrder.verify(governor, times(1)).notifyLastUpdatedChanged(any());
        inOrder.verify(governorListener, times(1)).lastUpdatedChanged(any());
        inOrder.verify(governor, times(1)).getLastActivity();
        verifyNoMoreInteractions(governor);
    }

    @Test
    public void testResetNotReady() throws Exception {
        Whitebox.setInternalState(governor, "bluetoothObject", null);
        governor.addGovernorListener(governorListener);

        governor.reset();

        // check interactions
        InOrder inOrder = inOrder(governor, governorListener);
        // side effect of using spy
        inOrder.verify(governor, times(1)).addGovernorListener(governorListener);

        // actual verification
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testResetReady() throws Exception {
        governor.addGovernorListener(governorListener);

        governor.reset();

        // check interactions
        InOrder inOrder = inOrder(governor, governorListener);
        inOrder.verify(governor, times(1)).reset(bluetoothObject);
        inOrder.verify(governorListener, times(1)).ready(false);

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testIsReady() {
        assertTrue(governor.isReady());

        Whitebox.setInternalState(governor, "bluetoothObject", null);
        assertFalse(governor.isReady());
    }

    @Test
    public void testNotifyLastUpdatedChanged() {
        Date date = new Date();
        governor.addGovernorListener(governorListener);

        governor.notifyLastUpdatedChanged(date);

        verify(governorListener, times(1)).lastUpdatedChanged(date);
    }

    @Test
    public void testNotifyLastUpdatedChangedException() {
        Date date = new Date();
        governor.addGovernorListener(governorListener);
        doThrow(Exception.class).when(governorListener).lastUpdatedChanged(any());

        governor.notifyLastUpdatedChanged(date);

        verify(governorListener, times(1)).lastUpdatedChanged(date);
    }

    @Test
    public void testNotifyReady() {
        governor.addGovernorListener(governorListener);

        governor.notifyReady(true);

        verify(governorListener, times(1)).ready(true);
    }

    @Test
    public void testNotifyReadyException() {
        governor.addGovernorListener(governorListener);
        doThrow(Exception.class).when(governorListener).ready(true);

        governor.notifyReady(true);

        verify(governorListener, times(1)).ready(true);
    }

}