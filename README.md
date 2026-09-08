# Floatation Server

## Launching of test rtsp stream

1. Download latest version from https://github.com/bluenviron/mediamtx/releases
2. Run `./mediamtx` in root of downloaded folder
3. Start publishing of video via `ffmpeg -re -stream_loop -1 -i "video.mp4" -c copy -f rtsp rtsp://localhost:8554/mystream`. Replace video.mp4 with full path to your video

## Run server with static distribution

> It is required to have installed:
> * JDK 17+
> * Docker engine
> * Docker compose (if you have old docker, for new one it is included in docker engine)

For running server you must build it:

```bash
#!/bin/bash

# In root of project
./gradlew :frameswork.server:build
```

Then in other terminal start postgres database:

```bash
#!/bin/bash

# In root of project
cd server
sudo docker compose up # or `sudo docker-compose up` for elder version od docker engine
```

And then run server:

```bash
#!/bin/bash

# In root of project

./gradlew run --args="sample.config.json" # It is better to use full path to file with config
```

Then you may open http://127.0.0.1:8196 to access web client
