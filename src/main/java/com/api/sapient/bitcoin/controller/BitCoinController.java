package com.api.sapient.bitcoin.controller;

import java.awt.List;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.api.sapient.bitcoin.response.BitcoinResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController(value = "sapient/api")
public class BitCoinController {
	
	@RequestMapping(value = "/bitcoin-report", method = RequestMethod.GET)
	public ResponseEntity<ArrayList<BitcoinResponse>> getBitcoinReport(
			@RequestParam("currency") String currency,
			@RequestParam("start_date") String startDate,
			@RequestParam("end_date") String endDate){
		
		String URL = "https://api.coindesk.com/v1/bpi/historical/close.json";
		
		ArrayList<BitcoinResponse> bitcoinResponseList = new ArrayList<BitcoinResponse>();
		
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(URL)
				.queryParam("currency", currency)
                .queryParam("start", startDate)
                .queryParam("end", endDate);
		
		System.out.println("URI: "+builder.toUriString());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        // Fetch the response data from Bitcoin API
        HttpEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);
        
        System.out.println("Response: "+response.getBody());
        
        if (((ResponseEntity<String>) response).getStatusCode() == HttpStatus.OK){
        	ObjectMapper mapper = new ObjectMapper();
        	mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        	try {
				JsonNode rootNode = mapper.readTree(response.getBody());
				String resString = rootNode.get("bpi").toString().replace("{", "").replace("}", "").replaceAll("\"","");
				System.out.println("resString: "+resString);
				String[] resArray = resString.split(",");
				
				Map<String, Double> map = new HashMap<String, Double>();
				
				for(String data : resArray){
					String[] datePriceArray = data.split(":");
					map.put(datePriceArray[0], Double.valueOf(datePriceArray[1]));			
				}
				String maxValueDate = Collections.max(map.entrySet(), Map.Entry.comparingByValue()).getKey();
				System.out.println("maxValueDate: "+maxValueDate);
				
				for (Map.Entry<String, Double> entry : map.entrySet())
				{
					BitcoinResponse res = new BitcoinResponse();
					
					if (entry.getKey().equals(maxValueDate))
						res.setPrice(entry.getValue()+" (highest)");
					else
						res.setPrice(entry.getValue().toString());
					
					res.setDate(stringToDate(entry.getKey()));
					
					res.setCurrency(currency);
					bitcoinResponseList.add(res);
				    
				}
				

				
			} catch (IOException e) {
				return new ResponseEntity(null, HttpStatus.NOT_FOUND);
			}
        }
	 return new ResponseEntity(bitcoinResponseList, HttpStatus.OK);
	}
	
	public static String stringToDate(String date)
    {
		SimpleDateFormat fromUser = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat myFormat = new SimpleDateFormat("dd-MM-yyyy");
		String reformattedStr = "";
        try {
        	 reformattedStr = myFormat.format(fromUser.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return reformattedStr;
    }

}
