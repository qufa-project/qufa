from django.urls import path
from Fairness import views


urlpatterns = [
    path('', views.index, name='index'),
    path('upload/', views.upload, name="upload"),
    path('overview/', views.overview, name="overview"),
    path('indicator/', views.indicator, name="indicator"),
]