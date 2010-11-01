package org.onebusaway.sound_transit.realtime.link;

import java.util.Comparator;

public class VehicleStateDescendingComparator implements
    Comparator<VehicleState> {

  public static final Comparator<VehicleState> INSTANCE = new VehicleStateDescendingComparator();

  @Override
  public int compare(VehicleState o1, VehicleState o2) {
    return o2.getEffectiveScheduleTime() - o1.getEffectiveScheduleTime();
  }
}
