#!/bin/bash

sudo docker run \
--gpus all \
-it \
-p 38888:8888 \
-v `pwd`/config-tf/tf:/tf \
-v `pwd`/config-tf/.jupyter:/root/.jupyter \
--name jupyter \
--restart=unless-stopped \
tensorflow/tensorflow:latest-gpu-py3-jupyter
