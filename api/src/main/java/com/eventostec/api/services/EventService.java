package com.eventostec.api.services;

import com.amazonaws.services.s3.AmazonS3;
import com.eventostec.api.domain.event.Event;
import com.eventostec.api.domain.event.EventRequestDTO;
import com.eventostec.api.domain.event.EventResponseDTO;
import com.eventostec.api.domain.repositories.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class EventService {

    @Value("${aws.bucket.name}")
    private String bucketName;

    @Autowired
    private AmazonS3 s3Client;

    @Autowired
    private EventRepository repository;

    @Autowired
    private AddressService addressService;

    public Event createEvent(EventRequestDTO data) {
        String imgUrl = null;

        if(data.image() != null) {
            imgUrl = this.uploadImg(data.image());
        }

        Event newEvent = new Event();
        newEvent.setTitle(data.title());
        newEvent.setDescription(data.description());
        newEvent.setEventUrl(data.eventUrl());
        newEvent.setDate(new Date(data.date()));
        newEvent.setImgUrl(imgUrl);
        newEvent.setRemote(data.remote());

        repository.save(newEvent); //para pegar o UUID

        if(!newEvent.getRemote()){
            this.addressService.createAddress(data, newEvent);
        }
        return newEvent;
    }

    private String uploadImg(MultipartFile multipartFile) {
        String fileName = UUID.randomUUID() + "-" + multipartFile.getOriginalFilename();
        try {
            File file = this.convertMultipartToFile(multipartFile); //Converted file into a file
            s3Client.putObject(bucketName, fileName, file);
            file.delete(); //get what was received from request, create a local arquive in my machine so that i can upload on s3 (convertMultipartToFile), then I delete it
            return s3Client.getUrl(bucketName, fileName).toString();
        } catch (Exception e) {
            System.out.println("Erro ao subir o arquivo.");
            return null;
        }
    }

    private File convertMultipartToFile(MultipartFile multipartFile) throws IOException {
        File convFile = new File(Objects.requireNonNull( multipartFile.getOriginalFilename() ));
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(multipartFile.getBytes());
        fos.close();
        return convFile;
    }

    public List<EventResponseDTO> getUpcomingEvents(int page, int size) { //getEvents
        Pageable pageable = PageRequest.of(page, size);
//        Page<Event> eventsPage = this.repository.findAll(pageable);
        Page<Event> eventsPage = this.repository.findUpcomingEvents(new Date(), pageable);//new Date()
        return eventsPage.map(event -> new EventResponseDTO(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getDate(),
                event.getAddress().getCity() != null ? event.getAddress().getCity() : "",
                event.getAddress().getUf() != null ? event.getAddress().getUf() : "",
                event.getRemote(),
                event.getEventUrl(),
                event.getImgUrl())).stream().toList();
    }

    public List<EventResponseDTO> getFilteredEvents(int page, int size, String title, String city, String uf, Date startDate, Date endDate) { //getEvents
        title = (title != null) ? title : "";
        city = (city != null) ? city : "";
        uf = (uf != null) ? uf : "";
        startDate = (startDate != null) ? startDate : new Date(0);
        LocalDate localEndDate = LocalDate.now().plusYears(10);
        endDate = (endDate != null) ? endDate : Date.from(localEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant());


        Pageable pageable = PageRequest.of(page, size);
        Page<Event> eventsPage = this.repository.filteredEvents(title, city, uf, startDate, endDate, pageable);//Page<Event> eventsPage = this.repository.filteredEvents(new Date(), title, city, uf, startDate, endDate, pageable);//new Date()
        return eventsPage.map(event -> new EventResponseDTO(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getDate(),
                event.getAddress().getCity() != null ? event.getAddress().getCity() : "",
                event.getAddress().getUf() != null ? event.getAddress().getCity() : "",
                event.getRemote(),
                event.getEventUrl(),
                event.getImgUrl())).stream().toList();
    }
 }