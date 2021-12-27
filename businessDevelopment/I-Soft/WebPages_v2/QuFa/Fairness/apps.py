from django.apps import AppConfig

import os
import tensorflow as tf

class FairnessConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'Fairness'
    
    os.environ['TF_XLA_FLAGS'] = '--tf_xla_enable_xla_devices'
    physical_devices = tf.config.list_physical_devices('GPU') 
    tf.config.experimental.set_memory_growth(physical_devices[0], True)
