package com.lucene.indexandsearch;


import java.io.*;
import java.util.Map;

import com.lucene.indexandsearch.utils.Constants;
import com.sun.xml.bind.v2.runtime.reflect.opt.Const;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ChartUtilities;

public class PlotGraph {

    public static void main(String[] args) throws Exception {

        RunTrecEval.runTrec();
        Map<Integer, Float> trecXYValues =  RunTrecEval.trecEvalXYValues;
        final XYSeries trecEvalMatrixValues = new XYSeries("");
        for (int i = 0; i < trecXYValues.size(); i++) {
            trecEvalMatrixValues.add(i, trecXYValues.get(i));
        }

        final XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(trecEvalMatrixValues);

        JFreeChart xylineChart = ChartFactory.createXYLineChart(
                "Precision VS Recall Curve",
                "Precision",
                "Recall",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        int width = 640;   /* Width of the image */
        int height = 480;  /* Height of the image */
        File XYChart = new File(Constants.precisionRecallGraphImagePath);
        ChartUtilities.saveChartAsJPEG(XYChart, xylineChart, width, height);
        System.out.println("Precision VS Recall graph image saved at: " + Constants.precisionRecallGraphImagePath);
        System.out.println("FINISHED!!");
    }
}