from django.urls import path
from Fairness import views


urlpatterns = [
    path('', views.index, name='index'),
    path('test/', views.test, name='test'),
    path('getkey/', views.getkey, name='getkey'),
    path('getcount/', views.getcount, name='getcount'),
    path('getlist/', views.getlist, name="getlist"),
    path('upload/', views.upload, name="upload"),
    path('loading/', views.loading, name="loading"),
    path('overview/', views.overview, name="overview"),
    path('indicator/', views.indicator, name="indicator"),
    path('isrunning/', views.isrunning, name="isrunning"),
    path('run/', views.run, name="run"),
    path('getresult/', views.getresult, name="getresult"),
]