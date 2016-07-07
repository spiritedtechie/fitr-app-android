package fitr.mobile.models;

import java.util.Date;

public class AggregatedDistance {

    private Date startDate;

    private Date endDate;

    private Float distanceInMeters;

    public AggregatedDistance(Date startDate, Date endDate, Float distanceInMeters) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.distanceInMeters = distanceInMeters;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Float getDistanceInMeters() {
        return distanceInMeters;
    }
}
