package org.onebusaway.sound_transit.realtime.link;

import org.onebusaway.gtfs.model.AgencyAndId;

public class VehicleInstance {

  private final AgencyAndId _vehicleId;

  private final VehicleState _state;

  public VehicleInstance(AgencyAndId vehicleId, VehicleState state) {
    _vehicleId = vehicleId;
    _state = state;
  }
  
  public AgencyAndId getVehicleId() {
    return _vehicleId;
  }
  
  public VehicleState getState() {
    return _state;
  }

}
