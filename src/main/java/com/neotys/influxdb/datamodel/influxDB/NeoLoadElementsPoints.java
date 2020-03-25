package com.neotys.influxdb.datamodel.influxDB;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;
import com.neotys.ascode.swagger.client.model.ElementDefinition;
import com.neotys.ascode.swagger.client.model.Point;
import com.neotys.ascode.swagger.client.model.TestDefinition;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Measurement(name = "neoload_ElementPoints",timeUnit = TimeUnit.MILLISECONDS)
public class NeoLoadElementsPoints extends NeoLoadElementData {
    @Column(name ="from")
    private Long from ;

    @Column(name ="to")
    private Long to ;

    @Column(name ="AVG_DURATION")
    private Double AVG_DURATION ;

    @Column(name ="MIN_DURATION")
    private Double MIN_DURATION ;

    @Column(name ="MAX_DURATION")
    private Double MAX_DURATION ;

    @Column(name ="COUNT")
    private Double COUNT ;

    @Column(name ="THROUGHPUT")
    private Double THROUGHPUT ;

    @Column(name ="ELEMENTS_PER_SECOND")
    private Double ELEMENTS_PER_SECOND ;

    @Column(name ="ERRORS")
    private Double ERRORS ;

    @Column(name ="ERRORS_PER_SECOND")
    private Double ERRORS_PER_SECOND ;

    @Column(name ="ERROR_RATE")
    private Double ERROR_RATE ;

    @Column(name ="AVG_TTFB")
    private Double AVG_TTFB ;

    @Column(name ="MIN_TTFB")
    private Double MIN_TTFB ;

    @Column(name ="MAX_TTFB")
    private Double MAX_TTFB ;




    public NeoLoadElementsPoints(TestDefinition testDefinition, ElementDefinition elementDefinition, Point point)
    {
       this.initialize(testDefinition);
       this.initElement(elementDefinition);
       this.to=point.getTo();
       this.from=point.getFrom();
       this.AVG_DURATION= setValue(point.getAVGDURATION());
       this.AVG_TTFB= setValue(point.getAVGTTFB());
       this.COUNT= setValue(point.getCOUNT());
       this.ELEMENTS_PER_SECOND= setValue(point.getELEMENTSPERSECOND());
       this.ERROR_RATE= setValue(point.getERRORRATE());
       this.ERRORS= setValue(point.getERRORS());
       this.ERRORS_PER_SECOND= setValue(point.getERRORSPERSECOND());
       this.MAX_DURATION= setValue(point.getMAXDURATION());
       this.MIN_DURATION= setValue(point.getMINDURATION());
       this.MAX_TTFB= setValue(point.getMAXTTFB());
       this.MIN_TTFB= setValue(point.getMINTTFB());
       this.THROUGHPUT=setValue(point.getTHROUGHPUT());
       this.time= Instant.ofEpochMilli(testDefinition.getStartDate()+this.to);

    }


    public Long getFrom() {
        return from;
    }

    public void setFrom(Long from) {
        this.from = from;
    }

    public Long getTo() {
        return to;
    }

    public void setTo(Long to) {
        this.to = to;
    }

    public Double getAVG_DURATION() {
        return AVG_DURATION;
    }

    public void setAVG_DURATION(Double AVG_DURATION) {
        this.AVG_DURATION = AVG_DURATION;
    }

    public Double getMIN_DURATION() {
        return MIN_DURATION;
    }

    public void setMIN_DURATION(Double MIN_DURATION) {
        this.MIN_DURATION = MIN_DURATION;
    }

    public Double getMAX_DURATION() {
        return MAX_DURATION;
    }

    public void setMAX_DURATION(Double MAX_DURATION) {
        this.MAX_DURATION = MAX_DURATION;
    }

    public Double getCOUNT() {
        return COUNT;
    }

    public void setCOUNT(Double COUNT) {
        this.COUNT = COUNT;
    }

    public Double getTHROUGHPUT() {
        return THROUGHPUT;
    }

    public void setTHROUGHPUT(Double THROUGHPUT) {
        this.THROUGHPUT = THROUGHPUT;
    }

    public Double getELEMENTS_PER_SECOND() {
        return ELEMENTS_PER_SECOND;
    }

    public void setELEMENTS_PER_SECOND(Double ELEMENTS_PER_SECOND) {
        this.ELEMENTS_PER_SECOND = ELEMENTS_PER_SECOND;
    }

    public Double getERRORS() {
        return ERRORS;
    }

    public void setERRORS(Double ERRORS) {
        this.ERRORS = ERRORS;
    }

    public Double getERRORS_PER_SECOND() {
        return ERRORS_PER_SECOND;
    }

    public void setERRORS_PER_SECOND(Double ERRORS_PER_SECOND) {
        this.ERRORS_PER_SECOND = ERRORS_PER_SECOND;
    }

    public Double getERROR_RATE() {
        return ERROR_RATE;
    }

    public void setERROR_RATE(Double ERROR_RATE) {
        this.ERROR_RATE = ERROR_RATE;
    }

    public Double getAVG_TTFB() {
        return AVG_TTFB;
    }

    public void setAVG_TTFB(Double AVG_TTFB) {
        this.AVG_TTFB = AVG_TTFB;
    }

    public Double getMIN_TTFB() {
        return MIN_TTFB;
    }

    public void setMIN_TTFB(Double MIN_TTFB) {
        this.MIN_TTFB = MIN_TTFB;
    }

    public Double getMAX_TTFB() {
        return MAX_TTFB;
    }

    public void setMAX_TTFB(Double MAX_TTFB) {
        this.MAX_TTFB = MAX_TTFB;
    }


}
