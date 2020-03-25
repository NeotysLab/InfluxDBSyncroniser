package com.neotys.influxdb.datamodel.influxDB;


import com.neotys.ascode.swagger.client.model.CounterDefinition;
import com.neotys.ascode.swagger.client.model.Point;
import com.neotys.ascode.swagger.client.model.TestDefinition;
import org.influxdb.annotation.Column;

import org.influxdb.annotation.Measurement;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

    @Measurement(name = "neoload_MonitoringPoints",timeUnit = TimeUnit.MILLISECONDS)
public class NeoLoadMonitoringPoints extends NeoLoadMonitoringData {
    @Column(name ="from")
    private Long from ;

    @Column(name ="to")
    private Long to ;

    @Column(name ="AVG")
    private Double AVG;

    public NeoLoadMonitoringPoints(TestDefinition definition, CounterDefinition counterDefinition, Point point)
    {
        this.initialize(definition);
        this.init_monitoringdata(counterDefinition);
        this.from=point.getFrom();
        this.to=point.getTo();
        this.AVG= setValue(point.getAVG());
        this.time= Instant.ofEpochMilli(definition.getStartDate()+ this.to);
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

    public Double getAVG() {
        return AVG;
    }

    public void setAVG(Double AVG) {
        this.AVG = AVG;
    }
}
