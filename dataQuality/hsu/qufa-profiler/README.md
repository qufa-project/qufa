# Profiler
> ``Data Infuser`` / Profiler 프로젝트 입니다.

Data Infuser 프로젝트에서 내 Loader 모듈이 프로파일링 작업을 DB Queue로 구현한 Job Queue에 삽입하면,  
스케줄러를 통해 자동으로 탐색하여 프로파일을 수행한 뒤 결과를 저장하는 기능을 맡은 모듈입니다.

모든 기능은 비동기 방식의 스케줄러가 일정 시간마다 Job Queue를 탐색하여 프로파일링 기능을 수행합니다.

## Environment
 * Spring boot v2.3.5
 * jdk 11.0.4
 * MariaDB v10.5.6
 * DataCleaner v5.7.1 lib (exclude : DataCleaner lib 내 slf4j, gson, elasticSearch 관련 lib; - Spring boot dependency와 충돌)

## Installation

 * DataCleaner 5.7.1 다운로드
    > https://github.com/datacleaner/DataCleaner/releases/download/DataCleaner-5.7.1/DataCleaner-5.7.1.zip

    > 다운로드 후 lib 디렉토리 내 파일들 -> Project root path에 libs 디렉토리 생성 후 이동
 * build.gradle -> dependencies 추가
    > compile group: 'org.mariadb.jdbc', name: 'mariadb-java-client', version: '2.6.0'

    > compile fileTree(dir: 'libs', include: ['*.jar'])

## Usage (배포 전)

Project > resources > config > datastore_config.xml을 사용 환경에 맞게 작성해야합니다. (sample 참고)

> run ProfilerApplication.java

ts-node-dev를 이용하여 실행하기 때문에 코드 수정 후 저장을 하는 경우 자동으로 재시작됩니다.

## How to run TEST

ProfileQueueService.java에 테스트용 메소드가 있습니다.
> DB에 테스트용 테이블을 생성한 뒤 메소드 내 테이블 명을 일치시켜주면 됩니다.

## BUILD and RUN For Production env

  배포 예정입니다.

### Environment variables

  배포 예정입니다.

## Meta

Promptechnology - [@Homepage](http://www.promptech.co.kr/) - [dev@promptech.co.kr](dev@promptech.co.kr)

프로젝트는 LGPL 3.0 라이센스로 개발되었습니다. 자세한 사항은 http://www.gnu.org/licenses/lgpl-3.0.txt 를 확인해주세요.

Licensed under the Lesser General Public License, See http://www.gnu.org/licenses/lgpl-3.0.txt for more information.

## Support
![alt text](http://wisepaip.org/assets/home/promptech-d8574a0910561aaea077bc759b1cf94c07baecc551f034ee9c7e830572d671de.png "Title Text")
