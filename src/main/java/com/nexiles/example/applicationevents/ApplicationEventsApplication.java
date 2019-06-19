package com.nexiles.example.applicationevents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class ApplicationEventsApplication {

    public ApplicationEventsApplication(Player player) {
        this.player = player;
    }

    public static void main(String[] args) {
        SpringApplication.run(ApplicationEventsApplication.class, args);
    }

    final Player player;

    @EventListener
    public void started(ApplicationStartedEvent event) {
        player.play();
    }

    @EventListener
    public void playerEventListener(PlayerEvent event) {
        log.info("Player Event: {}, time {}", event, event.getTime());
    }

    @EventListener
    public void playerStopEventListener(PlayerStopEvent event) {
        log.info("Player Event: {}, time {}, user {}", event, event.getTime(), event.getUser());
    }

    @EventListener
    public void timeTicked(PlayerTickEvent event) {
        log.info("Timer Tick: {}", event);

    }

    @EventListener
    public void playListEvent(PlayListTrackEvent event) {
        log.info("Playlist Event: {}: track {}", event, event.getTrack());

    }
}

class PlayerTickEvent extends ApplicationEvent {

    @Getter
    private Integer time;

    PlayerTickEvent(Object source, Integer time) {
        super(source);
        this.time = time;
    }

    @Override
    public String toString() {
        return "PlayerTickEvent: source= " + this.source.toString() + " time= " + this.time.toString();
    }
}

@Data
@AllArgsConstructor
class Track {
    private String name;
}

@Data
class PlayList {
    private List<Track> tracks;

    PlayList() {
        this.tracks = new ArrayList<>();
    }
}

class PlayListEvent extends ApplicationEvent {

    @Getter
    private PlayList playList;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     * @param playList
     */
    PlayListEvent(Object source, PlayList playList) {
        super(source);
        this.playList = playList;
    }
}

class PlayListAddedEvent extends PlayListEvent {
    /**
     * Create a new ApplicationEvent.
     *
     * @param source   the object on which the event initially occurred (never {@code null})
     * @param playList
     */
    public PlayListAddedEvent(Object source, PlayList playList) {
        super(source, playList);
    }
};

class PlayListRemovedEvent extends PlayListEvent {
    /**
     * Create a new ApplicationEvent.
     *
     * @param source   the object on which the event initially occurred (never {@code null})
     * @param playList
     */
    PlayListRemovedEvent(Object source, PlayList playList) {
        super(source, playList);
    }
};

class PlayListTrackEvent extends PlayListEvent {
    @Getter
    private Track track;
    /**
     * Create a new ApplicationEvent.
     *  @param source   the object on which the event initially occurred (never {@code null})
     * @param playList
     * @param track
     */
    PlayListTrackEvent(Object source, PlayList playList, Track track) {
        super(source, playList);
        this.track = track;
    }
};

@Service
class PlayListService {

    private PlayList current;
    private final ApplicationEventPublisher publisher;

    PlayListService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    void setCurrent(PlayList p) {
        publisher.publishEvent(new PlayListRemovedEvent(this, this.current));
       this.current = p;
        publisher.publishEvent(new PlayListAddedEvent(this, p));
    }

    void addTrack(Track track) {
        this.current.getTracks().add(track);
        publisher.publishEvent(new PlayListTrackEvent(this, this.current, track));
    }
}

class PlayerEvent extends ApplicationEvent {
    @Getter
    private Integer time;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     * @param time
     */
    PlayerEvent(Object source, Integer time) {
        super(source);
        this.time = time;
    }
}

class PlayerPlayEvent extends PlayerEvent {

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     * @param time
     */
    PlayerPlayEvent(Object source, Integer time) {
        super(source, time);
    }
}

class PlayerStopEvent extends PlayerEvent {
    @Getter
    private String user;
    /**
     * Create a new ApplicationEvent.
     *  @param source the object on which the event initially occurred (never {@code null})
     * @param time
     * @param user
     */
    PlayerStopEvent(Object source, Integer time, String user) {
        super(source, time);
        this.user = user;
    }
}


@Slf4j
@Service
class Player {
    @Getter
    private Integer time;

    @Getter
    private PlayList playList;

    private final ApplicationEventPublisher publisher;
    private final PlayListService playListService;

    @Getter
    @Setter
    private boolean playing;

    Player(ApplicationEventPublisher publisher, PlayListService playListService) {
        this.publisher = publisher;
        this.playListService = playListService;
        this.time = 0;

        this.playList = new PlayList();
        this.playListService.setCurrent(this.playList);
    }

    @Scheduled(fixedDelay = 2000)
    public void addTracks() {
        log.info("Adding new track!");
        this.playListService.addTrack(new Track(String.format("Track %d", this.time)));
    }

    void play() {
        this.setPlaying(true);
        publisher.publishEvent(new PlayerPlayEvent(this, this.time));
    }

    public void stop(String user) {
        this.setPlaying(false);
        publisher.publishEvent(new PlayerStopEvent(this, this.time, user));
    }

    @Scheduled(fixedDelay = 1000)
    public void ticker() {
        if (this.isPlaying()) {
            log.info("tick .... ");
            this.time += 1;
            publisher.publishEvent(new PlayerTickEvent(this, this.time));
        }
    }

}






