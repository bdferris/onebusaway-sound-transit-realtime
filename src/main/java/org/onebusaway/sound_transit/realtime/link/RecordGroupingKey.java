package org.onebusaway.sound_transit.realtime.link;

class RecordGroupingKey {

  private final double lat;

  private final double lon;

  private final int direction;

  public RecordGroupingKey(double lat, double lon, int direction) {
    this.lat = lat;
    this.lon = lon;
    this.direction = direction;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + direction;
    long temp;
    temp = Double.doubleToLongBits(lat);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(lon);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    RecordGroupingKey other = (RecordGroupingKey) obj;
    if (direction != other.direction)
      return false;
    if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
      return false;
    if (Double.doubleToLongBits(lon) != Double.doubleToLongBits(other.lon))
      return false;
    return true;
  }
}