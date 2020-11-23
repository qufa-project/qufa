# QuFa Visualization

1. Raw Data (.csv) 의 컬럼 지정 후 파일 Loading 및 parsing
2. 로딩된 Dataset 을 OverView 및 Facets Dive 로 가시화
3. 2개의 파일(Train/Test)까지 표현 가능

* Facets Overview :
    전체 데이터셋의 개요와 데이터 각각의 특징 면면에 대한 감을 제공
    개요에서는 각각의 특징에 대한 통계를 제공하고 훈련 및 검증 데이터셋의 비교 가능
* Facets Dive : 
    더 많은 정보를 얻기 위해 개별 특징에 대해 상세 식별 가능
    대규모의 데이터도 대화형 콘솔을 통해 한 번에 식별 가능
    
## Dataset
* 시카고시에서 발생한 교통사고 데이터 (부산대 제공)
    + 201008_origin_122219624.csv
    + 201008_origin_123605402.csv

## Development

### 웹 폴더 및 파일 구조
* /root
    + /css
        - qufa.css
    + /js
        - facets.html
        - facets.html.js
        - qufa.js
        - worker.js
    - index.html

### 개발 환경
* Browser :
    - Google Chrome v86.0
* Language :
    - HTML
    - JAVA SCRIPT
* Library : 
    - JQuery
    - Bootstrap
    - FontAwesome
    - ChartJS
    - D3
    - Google Facets

## Comment
* 현재 프로젝트 파일 다운로드 후 진입 파일(index.html)로 웹브라우저(Chrome에서만 동작)에서 실행 가능