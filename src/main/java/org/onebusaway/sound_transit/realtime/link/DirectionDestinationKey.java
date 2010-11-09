package org.onebusaway.sound_transit.realtime.link;

import org.onebusaway.gtfs.model.AgencyAndId;

class DirectionDestinationKey {

  private final AgencyAndId routeId;

  private final AgencyAndId destinationStopId;

  private final int direction;

  public DirectionDestinationKey(AgencyAndId routeId,
      AgencyAndId destinationStopId, int direction) {
    this.routeId = routeId;
    this.destinationStopId = destinationStopId;
    this.direction = direction;
  }

  public AgencyAndId getRouteId() {
    return routeId;
  }

  public AgencyAndId getDestinationStopId() {
    return destinationStopId;
  }

  public int getDirection() {
    return direction;
  }
  
  @Override
  public String toString() {
    return "DirectionDestinationKey(routeId=" + routeId + " destinationStopId=" + destinationStopId + " direction=" + direction +")";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((destinationStopId == null) ? 0 : destinationStopId.hashCode());
    result = prime * result + direction;
    result = prime * result + ((routeId == null) ? 0 : routeId.hashCode());
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
    DirectionDestinationKey other = (DirectionDestinationKey) obj;
    if (destinationStopId == null) {
      if (other.destinationStopId != null)
        return false;
    } else if (!destinationStopId.equals(other.destinationStopId))
      return false;
    if (direction != other.direction)
      return false;
    if (routeId == null) {
      if (other.routeId != null)
        return false;
    } else if (!routeId.equals(other.routeId))
      return false;
    return true;
  }
}