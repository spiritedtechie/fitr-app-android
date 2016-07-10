package fitr.mobile.views;

import com.github.mikephil.charting.data.BarData;

import java.util.List;

import fitr.mobile.models.AggregatedDistance;

public interface DistanceView {

    void setDistanceChartData(BarData barData);

    void setDistanceTableData(List<AggregatedDistance> data);

    void setRefreshing(boolean isRefreshing);
}
