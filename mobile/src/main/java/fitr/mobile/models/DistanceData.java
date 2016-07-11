package fitr.mobile.models;

import com.github.mikephil.charting.data.BarData;

import java.util.List;

public class DistanceData {

    private List<Distance> distances;

    private BarData distanceBarData;

    public DistanceData(List<Distance> distances, BarData distanceBarData) {
        this.distances = distances;
        this.distanceBarData = distanceBarData;
    }

    public List<Distance> getDistances() {
        return distances;
    }

    public BarData getDistanceBarData() {
        return distanceBarData;
    }
}
