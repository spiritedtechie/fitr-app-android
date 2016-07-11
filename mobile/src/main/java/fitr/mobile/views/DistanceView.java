package fitr.mobile.views;

import com.github.mikephil.charting.data.BarData;

import java.util.List;

import fitr.mobile.models.Distance;

public interface DistanceView extends View {

    void setDistanceChartData(BarData barData);

    void setDistanceTableData(List<Distance> data);

    void setRefreshing(boolean isRefreshing);
}
