package nextstep.subway.domain;

import javax.persistence.CascadeType;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Sections {

    @OneToMany(mappedBy = "line", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    public Sections() {
    }

    public void addSection(final Line line, final Station upStation, final Station downStation, final int distance) {
        if (this.sections.size() == 0) {
            this.sections.add(new Section(line, upStation, downStation, distance));
            return;
        }

        List<Station> allStations = allStations();

        if (bothStationsAreAlreadyInExistingStations(upStation, downStation, allStations)
                || nonStationsAreInExistingStations(upStation, downStation, allStations)) {
            throw new IllegalArgumentException();
        }

        if (isUpStationEndpoint(downStation) || isDownStationEndpoint(upStation)) {
            this.sections.add(new Section(line, upStation, downStation, distance));
            return;
        }

        Section sectionByUpStation = getSectionByUpStation(upStation);
        if (Objects.nonNull(sectionByUpStation)) {
            addBetweenSection(sectionByUpStation, sectionByUpStation.getUpStation(), downStation, sectionByUpStation.getDownStation(), distance, sectionByUpStation.getDistance() - distance);
            return;
        }

        Section sectionByDownStation = getSectionByDownStation(downStation);
        if (Objects.nonNull(sectionByDownStation)) {
            addBetweenSection(sectionByDownStation, sectionByDownStation.getUpStation(), upStation, downStation, sectionByDownStation.getDistance() - distance, distance);
        }
    }

    private boolean nonStationsAreInExistingStations(final Station upStation, final Station downStation, final List<Station> allStations) {
        return !(allStations.contains(upStation) || allStations.contains(downStation));
    }

    private boolean bothStationsAreAlreadyInExistingStations(final Station upStation, final Station downStation, final List<Station> allStations) {
        return allStations.contains(upStation) && allStations.contains(downStation);
    }

    private void addBetweenSection(Section section, Station upStation, Station middleStation, Station downStation, int distanceBetweenUpAndMiddleStation, int distanceBetweenMiddleAndDownStation) {
        if (distanceBetweenUpAndMiddleStation <= 0 || distanceBetweenMiddleAndDownStation <= 0) {
            throw new IllegalArgumentException();
        }

        this.sections.remove(section);
        this.sections.add(new Section(section.getLine(), upStation, middleStation, distanceBetweenUpAndMiddleStation));
        this.sections.add(new Section(section.getLine(), middleStation, downStation, distanceBetweenMiddleAndDownStation));
    }

    public void deleteSection(final Station station) {
        if (this.sections.size() == 0) {
            throw new IllegalArgumentException();
        }

        if (!isDownStationEndpoint(station)) {
            throw new IllegalArgumentException();
        }

        this.sections.remove(getSectionByDownStation(station));
    }

    public boolean isDownStationEndpoint(Station station) {
        return getDownStationEndpoint().equals(station);
    }

    private boolean isUpStationEndpoint(Station station) {
        return geUpStationEndpoint().equals(station);
    }

    public Station geUpStationEndpoint() {
        List<Station> upStations = sections.stream()
                .map(Section::getUpStation)
                .collect(Collectors.toList());
        List<Station> downStations = sections.stream()
                .map(Section::getDownStation)
                .collect(Collectors.toList());

        return upStations.stream()
                .filter(it -> !downStations.contains(it))
                .findAny()
                .orElse(null);
    }

    private Station getDownStationEndpoint() {
        List<Station> upStations = sections.stream()
                .map(Section::getUpStation)
                .collect(Collectors.toList());
        List<Station> downStations = sections.stream()
                .map(Section::getDownStation)
                .collect(Collectors.toList());

        return downStations.stream()
                .filter(it -> !upStations.contains(it))
                .findAny()
                .orElseThrow(IllegalArgumentException::new);
    }

    public Section getSectionByUpStation(Station station) {
        return this.sections.stream()
                .filter(it -> it.getUpStation().equals(station))
                .findAny()
                .orElse(null);
    }

    private Section getSectionByDownStation(Station station) {
        return this.sections.stream()
                .filter(it -> it.getDownStation().equals(station))
                .findAny()
                .orElse(null);
    }

    private List<Station> allStations() {
        List<Station> allStations = new ArrayList<>();

        List<Station> upStations = sections.stream()
                .map(Section::getUpStation)
                .collect(Collectors.toList());

        Station downStation = getDownStationEndpoint();

        allStations.addAll(upStations);
        allStations.add(downStation);

        return allStations;
    }
}