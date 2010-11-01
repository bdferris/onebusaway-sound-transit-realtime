package org.onebusaway.sound_transit.realtime.link;

import java.util.Comparator;

public class VehicleInstanceDescendingComparator implements
    Comparator<VehicleInstance> {

  public static final Comparator<VehicleInstance> INSTANCE = new VehicleInstanceDescendingComparator();

  @Override
  public int compare(VehicleInstance o1, VehicleInstance o2) {
    return VehicleStateDescendingComparator.INSTANCE.compare(o1.getState(),
        o2.getState());
  }
}
