package org.onebusaway.sound_transit.realtime.link;

import java.util.Comparator;

public class RecordDescendingTimepointArrivalTimeComparator implements
    Comparator<Record> {

  public static final Comparator<Record> INSTANCE = new RecordDescendingTimepointArrivalTimeComparator();

  @Override
  public int compare(Record r0, Record r1) {
    return r1.getTimepointArrivalTime() - r0.getTimepointArrivalTime();
  }
}
