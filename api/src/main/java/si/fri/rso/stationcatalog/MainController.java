package si.fri.rso.stationcatalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import si.fri.rso.stationcatalog.models.entities.Station;
import si.fri.rso.stationcatalog.models.entities.StationObject;
import si.fri.rso.stationcatalog.services.StationService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RestController // This means that this class is a Controller
@RefreshScope
@CrossOrigin(origins = "https://frontend-t4vmtoqorq-ew.a.run.app")
public class MainController {

    private static final Log LOGGER = LogFactory.getLog(MainController.class);

    @Value("${allowCreation:true}")
    private boolean canCreate;

    @Value("${API_KEY}")
    private String api_key;

    @Autowired
    private StationService stationService;

    @GetMapping("/")
    public ResponseEntity healthCheck(){
        return ResponseEntity.ok().build();
    }

    @PostMapping(path="/station/add") // Map ONLY POST Requests
    @ResponseBody
    public ResponseEntity addNewStation (@RequestBody StationObject stationObject) {
        // @ResponseBody means the returned String is the response, not a view name
        // @RequestParam means it is a parameter from the GET or POST request
        LOGGER.info("Entered addNewStation method");
        if(canCreate == false){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Station n = new Station();
        n.setOwner(stationObject.owner);
        n.setLat(stationObject.lat);
        n.setLon(stationObject.lon);
        n.setReserved(false);
        stationService.addStation(n);
        LOGGER.info("Exited addNewStation method");
        return ResponseEntity.ok().build();
    }

    @GetMapping(path="/station/all")
    public @ResponseBody List<Station> getAllStations(@RequestParam double lat, @RequestParam double lon) {
        LOGGER.info("Entered getAllStations method");
        Iterable<Station> s = stationService.getAllStations().join();
        Iterator<Station> iter = s.iterator();

        List<Station> end = new ArrayList<Station>();

        String destinations = "";

        while(iter.hasNext()){
            Station next = iter.next();
            destinations += next.getLat() + "%2C" + next.getLon() + "%7C";
            end.add(next);
        }

        destinations = destinations.substring(0, destinations.length() - 3);

        String origins = lat+"%2C"+lon;

        String uri = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + origins + "&destinations=" + destinations + "&key=" + api_key;

        String result = "";
        try {
            URL url = new URL(uri);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            con.setConnectTimeout(5000);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            result = content.toString();

            con.disconnect();
        }catch (Exception e){

        }

        ObjectMapper objectMapper = new ObjectMapper();

        DistanceMatrixResponse response = null;

        boolean success = false;

        try {
            response = objectMapper.readValue(result, DistanceMatrixResponse.class);
            success = true;
        }catch (Exception e){
            System.out.println(e);
        }

        if(success == true){
            List<Station> endResult = new ArrayList<Station>();


            int num = 10;

            if(end.size() < 10)
                num = end.size();

            for(int k = 0; k < num; k++) {
                int smallest = Integer.MAX_VALUE;
                int index = 0;
                for (int i = 0; i < response.rows[0].elements.length; i++) {
                    if (response.rows[0].elements[i].distance.value <= smallest) {
                        if(!isAlreadyInList(endResult, end.get(i))) {
                            smallest = response.rows[0].elements[i].distance.value;
                            index = i;
                        }
                    }
                }
                endResult.add(end.get(index));
            }
            LOGGER.info("Exited getAllStations method");
            return endResult;
        }

        LOGGER.error("Exited getAllStations method | api data error");

        return end;
    }

    private boolean isAlreadyInList(List<Station> list, Station s){
        for(int i = 0; i < list.size(); i++){
            if(s.getId() == list.get(i).getId()){
                return true;
            }
        }
        return false;
    }
}