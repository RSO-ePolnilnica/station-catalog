package si.fri.rso.stationcatalog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import si.fri.rso.stationcatalog.models.entities.Station;
import si.fri.rso.stationcatalog.services.StationService;

@Controller // This means that this class is a Controller
@RequestMapping(path="/station") // This means URL's start with /demo (after Application path)
public class MainController {

    @Autowired
    private StationService stationService;

    @PostMapping(path="/add") // Map ONLY POST Requests
    public @ResponseBody String addNewUser (@RequestParam String owner, @RequestParam double lat, @RequestParam double lon) {
        // @ResponseBody means the returned String is the response, not a view name
        // @RequestParam means it is a parameter from the GET or POST request

        Station n = new Station();
        n.setOwner(owner);
        n.setLat(lat);
        n.setLon(lon);
        n.setReserved(false);
        stationService.addStation(n);
        return "Saved";
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<Station> getAllUsers() {
        // This returns a JSON or XML with the users
        return stationService.getAllStations();
    }
}