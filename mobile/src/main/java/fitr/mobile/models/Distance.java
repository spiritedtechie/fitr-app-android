package fitr.mobile.models;

import java.util.Date;

public class Distance {

    private Date startDate;

    private Date endDate;

    private Float distanceInMeters;

    public Distance(Date startDate, Date endDate, Float distanceInMeters) {
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
