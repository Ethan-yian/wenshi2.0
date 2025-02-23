package com.example.wenshi;

import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class CustomMarkerView extends MarkerView {
    private TextView tvContent;
    private TextView tvTime;

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
        tvTime = findViewById(R.id.tvTime);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (highlight.getDataSetIndex() == 0) {
            tvContent.setText(String.format("温度: %.1f°C", e.getY()));
        } else {
            tvContent.setText(String.format("湿度: %.1f%%", e.getY()));
        }
        tvTime.setText("时间: " + getXLabel(e.getX()));
        super.refreshContent(e, highlight);
    }

    private String getXLabel(float x) {
        if (getChartView() != null) {
            return getChartView().getXAxis().getValueFormatter().getFormattedValue(x);
        }
        return "";
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}