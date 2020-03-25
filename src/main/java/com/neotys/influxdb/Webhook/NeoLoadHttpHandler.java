package com.neotys.influxdb.Webhook;


import com.neotys.ascode.swagger.client.ApiClient;
import com.neotys.ascode.swagger.client.ApiException;
import com.neotys.ascode.swagger.client.api.ResultsApi;
import com.neotys.ascode.swagger.client.model.*;
import com.neotys.influxdb.Logger.NeoLoadLogger;
import com.neotys.influxdb.conf.NeoLoadException;
import com.neotys.influxdb.datamodel.influxDB.*;
import io.vertx.core.Future;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.impl.InfluxDBMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.neotys.influxdb.conf.Constants.*;

public class NeoLoadHttpHandler {
    private String testid;
    private Optional<String> neoload_Web_Url;
    private Optional<String> neoload_API_Url;
    private String neoload_API_key;
    private NeoLoadLogger logger;
    private ApiClient apiClient;
    private Optional<String> influxDBHost;
    private Optional<String> influxDBport;
    private Optional<String> influxDatabase;
    private Optional<String> influxDB_user;
    private Optional<String> influxDB_pwd;
    private ResultsApi resultsApi;
    private String projectName;
    private String scenarioName;
    private String testname;
    private long testStart;
    private long testEnd;
    private String status;
    private TestStatistics statistics;
    private String maxVu;
    private String testoverviewpng;
    private boolean ssl;
    private InfluxDB influxDB;
    private  InfluxDBMapper influxDBMapper;

    public NeoLoadHttpHandler(String testid) throws NeoLoadException {
        testid=testid;
        logger=new NeoLoadLogger(this.getClass().getName());
        logger.setTestid(testid);
        getEnvVariables();


        generateApiUrl();

        apiClient=new ApiClient();
        apiClient.setBasePath(HTTPS+neoload_API_Url.get());
        apiClient.setApiKey(neoload_API_key);
        resultsApi=new ResultsApi(apiClient);

        connectDB();
    }

    private void connectDB()
    {
        this.influxDB= InfluxDBFactory.connect(generateInfluxDBUrl(),influxDB_user.get(),influxDB_pwd.get());
        if(this.influxDB.describeDatabases().contains(influxDatabase.get()))
        {
            logger.debug("Database "+influxDatabase.get()+" exists");
            this.influxDB.setDatabase(influxDatabase.get());
        }
        else
        {
            logger.debug("Database "+influxDatabase.get()+" does not exists");
            this.influxDB.createDatabase(influxDatabase.get());
            logger.debug("Database "+influxDatabase.get()+" created");

        }

        this.influxDBMapper = new InfluxDBMapper(influxDB);
    }
    private String generateInfluxDBUrl()
    {
        if(influxDBHost.isPresent())
        {
            if(influxDBport.isPresent())
            {
                return "http://"+influxDBHost.get()+":"+influxDBport.get();
            }
            else
                return null;
        }
        else
            return null;
    }
    public Future<Boolean> syncTestData()
    {
        Future<Boolean> future_results= Future.future();
        String test_status=null;
        AtomicReference<Integer> offset_events= new AtomicReference<Integer>( 0);
        AtomicReference<Long> offset_elements=new AtomicReference<Long>((long) 0);
        AtomicReference<Long> offset_monitor=new AtomicReference<Long>((long) 0);

        List<String> errorStrings=new ArrayList<>();
        try {
            TestDefinition testDefinition = resultsApi.getTest(testid);
            while (test_status==null && !test_status.equalsIgnoreCase(NEOLOAD_ENDSTATUS))
            {
                testDefinition = resultsApi.getTest(testid);
                test_status=testDefinition.getStatus().getValue();
                TestDefinition finalTestDefinition2 = testDefinition;

                ELEMENT_LIST_CATEGORY.stream().parallel().forEach(category ->
                {
                    try {
                        resultsApi.getTestElements(testid, category).forEach(elementDefinition ->
                        {
                            //----for each element-----
                            try {
                                resultsApi.getTestElementsPoints(testid, elementDefinition.getId(), ELEMENT_STATISTICS).forEach(point ->
                                {
                                    //----store the points----
                                    if(point.getFrom()>offset_elements.get())
                                    {
                                        NeoLoadElementsPoints elementsPoints=new NeoLoadElementsPoints(finalTestDefinition2,elementDefinition,point);
                                        influxDBMapper.save(elementsPoints);
                                    }
                                    //-----------------------
                                    offset_elements.set(point.getTo());
                                });
                            } catch (ApiException e) {
                                logger.error("Error parsing the element for id "+elementDefinition.getId(), e);
                                errorStrings.add("Error parsing the element for id "+elementDefinition.getId() +" -" +e.getMessage());
                            }
                        });
                    } catch (ApiException e) {
                        logger.error("Error parseing results", e);
                        errorStrings.add("Error parseing results "+e.getMessage());
                    }

                    try{
                    //----query the coutners$
                    resultsApi.getTestMonitors(testid).forEach(counterDefinition ->
                    {

                        try {
                            resultsApi.getTestMonitorsPoints(testid, counterDefinition.getId()).forEach(point ->
                            {
                                //------store in the database------
                                if(point.getFrom()>offset_monitor.get())
                                {
                                    NeoLoadMonitoringPoints monitoringPoints=new NeoLoadMonitoringPoints(finalTestDefinition2,counterDefinition,point);
                                    influxDBMapper.save(monitoringPoints);

                                }
                                offset_monitor.set(point.getTo());
                                //-------------------------------
                            });
                        }
                        catch (ApiException e)
                        {
                            logger.error("unable to qery the points of the counter : " + counterDefinition.getId(),e);
                            errorStrings.add("unable to qery the points of the counter : " + counterDefinition.getId() + " - "+e.getMessage());
                        }
                    });
                    }
                    catch (ApiException e)
                    {
                        logger.error("unable to qery counter ",e);
                        errorStrings.add("unable to qery counter "+e.getMessage());

                    }

                });
                HashMap<String,String> elements=new HashMap<>();
                TestDefinition finalTestDefinition = testDefinition;
                resultsApi.getTestEvents(testid,null,200, offset_events.get(),"+offset_events").forEach(eventDefinition ->
                {
                    String elementname=elements.get(eventDefinition.getElementid().toString());
                    if(elementname==null)
                    {
                        try
                        {
                            elementname=resultsApi.getTestElementDefinition(testid,eventDefinition.getElementid().toString()).getName();
                            elements.put(eventDefinition.getElementid().toString(),elementname);
                        }
                        catch (ApiException e)
                        {
                            logger.error("Unable to find the element "+eventDefinition.getElementid().toString());
                            errorStrings.add("unable to find the element  : " + eventDefinition.getId() + " - "+e.getMessage());

                        }

                    }
                    //----store the event---------------
                    NeoLoadEvents neoLoadEvents=new NeoLoadEvents(finalTestDefinition,eventDefinition,elementname);
                    influxDBMapper.save(neoLoadEvents);
                    offset_events.set(Integer.parseInt(eventDefinition.getId()));
                    //----------------------------------
                });
            }

            if(test_status.equalsIgnoreCase(NEOLOAD_ENDSTATUS))
            {
                //---get the values-----
                TestDefinition finalTestDefinition1 = testDefinition;
                ELEMENT_LIST_CATEGORY.stream().parallel().forEach(category ->
                {
                    try {
                        resultsApi.getTestElements(testid, category).forEach(elementDefinition ->
                        {
                            //----for each element-----
                            try {
                                ElementValues values=resultsApi.getTestElementsValues(testid, elementDefinition.getId());
                                //----store the element value--
                                NeoLoadElementsValues neoLoadElementsValues=new NeoLoadElementsValues(finalTestDefinition1,elementDefinition,values);
                                influxDBMapper.save(neoLoadElementsValues);

                                //-----------------------------
                            } catch (ApiException e) {
                                logger.error("Error parsing the element values for id "+elementDefinition.getId(), e);
                                errorStrings.add("unable to find the element  : " + elementDefinition.getId() + " - "+e.getMessage());

                            }
                        });
                        resultsApi.getTestMonitors(testid).forEach(counterDefinition -> {
                            try {
                                CounterValues customMonitorValues=resultsApi.getTestMonitorsValues(testid, counterDefinition.getId());
                                NeoLoadMonitoringValues neoLoadMonitoringValues=new NeoLoadMonitoringValues(finalTestDefinition1,counterDefinition,customMonitorValues);
                                influxDBMapper.save(neoLoadMonitoringValues);
                                //----store the monitoring value-------
                            }
                            catch (ApiException e)
                            {
                                logger.error("unable to find counter " + counterDefinition.getId());
                                errorStrings.add("unable to find the counter  : " + counterDefinition.getId() + " - "+e.getMessage());

                            }
                        });
                    } catch (ApiException e) {
                        logger.error("Errror parseing results", e);
                        errorStrings.add("unable to parse results   - "+e.getMessage());

                    }

                });
            }
            if(errorStrings.size()>0)
            {
                future_results.fail(errorStrings.stream().collect(Collectors.joining(",")));
            }
            else
            {
                influxDB.close();
                future_results.complete(true);
            }

        }
        catch (ApiException e)
        {
            logger.error("Error during syncTestdata",e);
            influxDB.close();
            future_results.fail(e);
        }
        return future_results;
    }

    private void generateApiUrl()
    {
        if(neoload_API_Url.isPresent())
        {
            if(!neoload_API_Url.get().contains(API_URL_VERSION))
                neoload_API_Url=Optional.of(neoload_API_Url.get()+API_URL_VERSION);
        }
    }
    private void getEnvVariables() throws NeoLoadException {

        logger.debug("retrieve the environement variables for neoload  neoload service ");
        neoload_API_key=System.getenv(SECRET_API_TOKEN);
        if(neoload_API_key==null)
        {
            logger.error("No API key defined");
            throw new NeoLoadException("No API key is defined");
        }
        neoload_API_Url= Optional.ofNullable(System.getenv(SECRET_NL_API_HOST)).filter(o->!o.isEmpty());
        if(!neoload_API_Url.isPresent())
            neoload_API_Url=Optional.of(DEFAULT_NL_SAAS_API_URL);

        neoload_Web_Url=Optional.ofNullable(System.getenv(SECRET_NL_WEB_HOST)).filter(o->!o.isEmpty());
        if(!neoload_Web_Url.isPresent())
            neoload_Web_Url=Optional.of(SECRET_NL_WEB_HOST);

        if(System.getenv(SECRET_SSL)!=null&& !System.getenv(SECRET_SSL).isEmpty())
        {
            ssl=Boolean.parseBoolean(System.getenv(SECRET_SSL));

        }
        else
            ssl=false;



        influxDBHost=Optional.ofNullable(System.getenv(SECRET_INFLUXDB_HOST)).filter(o->!o.isEmpty());
        if(!influxDBHost.isPresent()) {
            throw new NeoLoadException("The InfluxDB Host is required");
        }
        else
            logger.debug("INFLUXDB hostname is defined");

        influxDBport=Optional.ofNullable(System.getenv(SECRET_INFLUXDB_PORT)).filter(o->!o.isEmpty());
        if(!influxDBport.isPresent())
            influxDBport=Optional.of(DEFAULT_INFLUXDB_PORT);

        influxDatabase=Optional.ofNullable(System.getenv(SECRET_INFLUXDB_DATABASE)).filter(o->!o.isEmpty());
        if(!influxDatabase.isPresent())
            throw new NeoLoadException("INfluxDB port is required");

        influxDB_user =Optional.ofNullable(System.getenv(SECRET_INFLUXDB_USER)).filter(o->!o.isEmpty());
        if(!influxDB_user.isPresent())
            throw new NeoLoadException("The user environment varaible is missing");

        influxDB_pwd =Optional.ofNullable(System.getenv(SECRET_INFLUXDB_PASSWORD)).filter(o->!o.isEmpty());
        if(!influxDB_pwd.isPresent())
            throw new NeoLoadException("The password environment varaible is missing");
    }
}