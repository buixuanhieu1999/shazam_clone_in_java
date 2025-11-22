




- **Audio Fingerprinting**: Uses FFT-based spectral analysis to create unique audio fingerprints.
- **Persistent Storage**: Stores songs and fingerprints in a MySQL database.
- **Fast Matching**: Efficient hash lookups with time-consistency scoring.
- **Microphone Support**: Record and identify songs in real-time.
- **File Analysis**: Identify songs from WAV audio files.
- **Robustness**: Handles noise and slight distortions.


## Installation & Setup

### 1. Database Setup

1.  Install MySQL Server if you haven't already.
2.  Create a database (optional, the application can create it for you if your user has permissions):
    ```sql
    CREATE DATABASE shazam_db;
    ```
3.  Create a `.env` file in the project root directory with your database credentials:
    ```env
    DB_URL=jdbc:mysql://localhost:3306/shazam_db?createDatabaseIfNotExist=true
    DB_USER=root
    DB_PASS=your_password
    ```

### 2. Build the Project

```bash
mvn clean package
```

This creates an executable JAR file in the `target` directory: `target/shazam-clone-1.0-SNAPSHOT.jar`.

## Usage

### 1. Index Songs

Add songs to the fingerprint database. The system auto-detects "Artist - Title" from filenames.

```bash
# Index all WAV files in a directory
java -jar target/shazam-clone-1.0-SNAPSHOT.jar index ./music-library

# Index a single file
java -jar target/shazam-clone-1.0-SNAPSHOT.jar index song.wav
```

### 2. Add Song with Metadata

Manually specify artist and title:

```bash
java -jar target/shazam-clone-1.0-SNAPSHOT.jar add song.wav "The Beatles" "Hey Jude"
```

### 3. Identify from Microphone

Record audio for a few seconds and identify the song:

```bash
# Record for 10 seconds (default)
java -jar target/shazam-clone-1.0-SNAPSHOT.jar listen

# Record for custom duration (e.g., 15 seconds)
java -jar target/shazam-clone-1.0-SNAPSHOT.jar listen 15
```

### 4. Identify from File

Match an existing audio file against the database:

```bash
java -jar target/shazam-clone-1.0-SNAPSHOT.jar identify query_snippet.wav
```

### 5. List Songs

View all indexed songs in the database:

```bash
java -jar target/shazam-clone-1.0-SNAPSHOT.jar list
```

## How It Works

1.  **Spectrogram Generation**: The audio is converted into a spectrogram using Short-Time Fourier Transform (STFT).
2.  **Peak Detection**: The algorithm finds local peaks in the spectrogram (points with higher energy than their neighbors).
3.  **Fingerprint Hashing**: Pairs of peaks (an anchor and a target) are combined to form a hash. The hash includes the frequencies of both peaks and the time difference between them.
4.  **Database Storage**: These hashes are stored in MySQL, linked to the song ID and the time offset.
5.  **Matching**: When identifying, the same process generates hashes for the query audio. The system looks up these hashes in the database and calculates a "time consistency score" to find the best match.

## Troubleshooting

-   **Database Connection Error**: Ensure MySQL is running and your `.env` file has the correct credentials.
-   **Microphone Not Working**: Ensure your microphone is set as the default recording device in your OS settings.
-   **Low Confidence**: Background noise can lower confidence scores. Try recording closer to the source or for a longer duration.

## License

This project is for educational purposes.
