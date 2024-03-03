package com.driver;

import java.util.*;

import java.util.stream.Collectors;

public class SpotifyRepository {
    public HashMap<Artist, List<Album>> artistAlbumMap;
    public HashMap<Album, List<Song>> albumSongMap;
    public HashMap<Playlist, List<Song>> playlistSongMap;
    public HashMap<Playlist, List<User>> playlistListenerMap;
    public HashMap<User, Playlist> creatorPlaylistMap;
    public HashMap<User, List<Playlist>> userPlaylistMap;
    public HashMap<Song, List<User>> songLikeMap;

    public List<User> users;
    public List<Song> songs;
    public List<Playlist> playlists;
    public List<Album> albums;
    public List<Artist> artists;

    public SpotifyRepository() {
        artistAlbumMap = new HashMap<>();
        albumSongMap = new HashMap<>();
        playlistSongMap = new HashMap<>();
        playlistListenerMap = new HashMap<>();
        creatorPlaylistMap = new HashMap<>();
        userPlaylistMap = new HashMap<>();
        songLikeMap = new HashMap<>();

        users = new ArrayList<>();
        songs = new ArrayList<>();
        playlists = new ArrayList<>();
        albums = new ArrayList<>();
        artists = new ArrayList<>();
    }

    public User createUser(String name, String mobile) {
        User existingUser = findUserByMobile(mobile);
        if (existingUser != null) {
            throw new RuntimeException("User with mobile number " + mobile + " already exists.");
        }

        User user = new User(name, mobile);
        users.add(user);
        return user;
    }

    public Artist createArtist(String name) {
        Artist existingArtist = findArtistByName(name);
        if (existingArtist != null) {
            throw new RuntimeException("Artist with name " + name + " already exists.");
        }

        Artist artist = new Artist(name);
        artists.add(artist);
        return artist;
    }

    public Album createAlbum(String title, String artistName) {
        Artist artist = findOrCreateArtist(artistName);
        Album existingAlbum = findAlbum(title);
        if (existingAlbum != null) {
            throw new RuntimeException("Album with title " + title + " already exists.");
        }

        Album album = new Album(title);
        artistAlbumMap.computeIfAbsent(artist, k -> new ArrayList<>()).add(album);
        albums.add(album);
        return album;
    }

    public Song createSong(String title, String albumName, int length) throws Exception {
        Album album = findAlbum(albumName);
        if (album == null) {
            throw new RuntimeException("Album with title " + albumName + " does not exist.");
        }

        Song existingSong = findSong(title);
        if (existingSong != null) {
            throw new RuntimeException("Song with title " + title + " already exists.");
        }

        Song song = new Song(title, length);
        albumSongMap.computeIfAbsent(album, k -> new ArrayList<>()).add(song);
        songs.add(song);
        return song;
    }

    public Playlist createPlaylistOnLength(String mobile, String title, int length) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new RuntimeException("User with mobile number " + mobile + " does not exist.");
        }

        Playlist existingPlaylist = findPlaylistByTitle(title);
        if (existingPlaylist != null) {
            throw new RuntimeException("Playlist with title " + title + " already exists.");
        }

        Playlist playlist = new Playlist(title);
        List<Song> songsToAdd = songs.stream()
                .filter(song -> song.getLength() == length)
                .collect(Collectors.toList());

        playlistSongMap.put(playlist, songsToAdd);
        playlistListenerMap.put(playlist, List.of(user));
        creatorPlaylistMap.put(user, playlist);
        userPlaylistMap.computeIfAbsent(user, k -> new ArrayList<>()).add(playlist);
        return playlist;
    }

    public Playlist createPlaylistOnName(String mobile, String title, List<String> songTitles) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new RuntimeException("User with mobile number " + mobile + " does not exist.");
        }

        Playlist existingPlaylist = findPlaylistByTitle(title);
        if (existingPlaylist != null) {
            throw new RuntimeException("Playlist with title " + title + " already exists.");
        }

        Playlist playlist = new Playlist(title);
        List<Song> songsToAdd = songs.stream()
                .filter(song -> songTitles.contains(song.getTitle()))
                .collect(Collectors.toList());

        playlistSongMap.put(playlist, songsToAdd);
        playlistListenerMap.put(playlist, List.of(user));
        creatorPlaylistMap.put(user, playlist);
        userPlaylistMap.computeIfAbsent(user, k -> new ArrayList<>()).add(playlist);
        return playlist;
    }

    public Playlist findPlaylist(String mobile, String playlistTitle) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new RuntimeException("User with mobile number " + mobile + " does not exist.");
        }

        Playlist playlist = findPlaylistByTitle(playlistTitle);
        if (playlist == null) {
            throw new RuntimeException("Playlist with title " + playlistTitle + " does not exist.");
        }

        if (!playlistListenerMap.get(playlist).contains(user) && !user.equals(creatorPlaylistMap.get(playlist))) {
            playlistListenerMap.computeIfAbsent(playlist, k -> new ArrayList<>()).add(user);
            userPlaylistMap.computeIfAbsent(user, k -> new ArrayList<>()).add(playlist);
        }

        return playlist;
    }

    public Song likeSong(String mobile, String songTitle) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new RuntimeException("User with mobile number " + mobile + " does not exist.");
        }

        Song song = findSong(songTitle);
        if (song == null) {
            throw new RuntimeException("Song with title " + songTitle + " does not exist.");
        }

        if (!songLikeMap.get(song).contains(user)) {
            songLikeMap.computeIfAbsent(song, k -> new ArrayList<>()).add(user);
            Artist artist = findArtistBySong(song);
            if (artist != null) {
                artist.setLikes(artist.getLikes() + 1);
            }
        }

        return song;
    }

    public String mostPopularArtist() {
        Artist mostPopular = artists.stream()
                .max(Comparator.comparingInt(Artist::getLikes))
                .orElse(null);

        return mostPopular != null ? mostPopular.getName() : "No popular artist";
    }

    public String mostPopularSong() {
        Song mostPopular = songs.stream()
                .max(Comparator.comparingInt(song -> songLikeMap.getOrDefault(song, Collections.emptyList()).size()))
                .orElse(null);

        return mostPopular != null ? mostPopular.getTitle() : "No popular song";
    }

    // Helper methods
    private Artist findOrCreateArtist(String name) {
        return artists.stream()
                .filter(artist -> artist.getName().equals(name))
                .findFirst()
                .orElseGet(() -> createArtist(name));
    }

    private Album findAlbum(String title) {
        return albums.stream()
                .filter(album -> album.getTitle().equals(title))
                .findFirst()
                .orElse(null);
    }

    private User findUserByMobile(String mobile) {
        return users.stream()
                .filter(user -> user.getMobile().equals(mobile))
                .findFirst()
                .orElse(null);
    }

    private Song findSong(String title) {
        return songs.stream()
                .filter(song -> song.getTitle().equals(title))
                .findFirst()
                .orElse(null);
    }

    private Artist findArtistBySong(Song song) {
        return artistAlbumMap.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(album -> albumSongMap.get(album).contains(song)))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private Artist findArtistByName(String name) {
        return artists.stream()
                .filter(artist -> artist.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private Playlist findPlaylistByTitle(String title) {
        return playlists.stream()
                .filter(playlist -> playlist.getTitle().equals(title))
                .findFirst()
                .orElse(null);
    }
}
