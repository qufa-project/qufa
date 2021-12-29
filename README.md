# **QuFa** (Data **Qu**ality and **Fa**irness)

- 데이터 품질 평가기반 데이터 고도화 및 데이터셋 보정 기술 개발 (Development of data improvement and dataset correction technology based on data quality assessment)

- 2020 정보통신·방송 연구개발 사업 (SW컴퓨팅산업원천기술개발)

- Organization

| 구분 | 기관명 |
| ------ | ------ |
| 전문기관 | 정보통신기획평가원 (IITP) |
| 주관기관 | 부산대학교 |
| 공동연구기관 | 서울대학교, 서울시립대학교, 이화여자대학교, 한성대학교, (주)프람트테크놀로지, (주)아이소프트 |
| 수요기업 | (주)프람트테크놀로지, (주)아이소프트 |

##

<ul>
    <li>
      <label>QuFa (Repository - structure of directories)</label>
      <ul>
        <li>
          <label>dataDomain (데이터 셋)</label>
          <ul>
            <li>
                <label>traffic (교통 데이터/부산대학교) - <a href="https://gitlab.com/qufa/qufa/-/tree/master/dataDomain/traffic/dataSet">data set</a></label>
            </li>
            <li>
                <label>health (보건 데이터/서울대학교) - <a href="https://gitlab.com/qufa/qufa/-/tree/master/dataDomain/health/dataSet">data set</a></label>
            </li>
            <li>
                <label>culturalTourism (문화관광 데이터/서울시립대학교) - <a href="https://gitlab.com/qufa/qufa/-/tree/master/dataDomain/culturalTourism/dataSet">data set</a></label>
            </li>
            <li>
                <label>environment (환경 데이터/이화여자대학교) - <a href="https://gitlab.com/qufa/qufa/-/tree/master/dataDomain/environment/dataSet">data set</a></label>
            </li>
            <li>
                <label>disasterSafety (재난안전 데이터/한성대학교) - <a href="https://gitlab.com/qufa/qufa/-/tree/master/dataDomain/disasterSafety/dataSet">data set</a></label>
            </li>
          </ul>
        </li><br>
        <li>
          <label>dataFairness (데이터 공정성팀)</label>
          <ul>
            <li>
                <label>pnu (부산대학교) - <a href=https://gitlab.com/qufa/qufa/-/tree/master/dataFairness/pnu>source code</a></label>
            </li>
            <li>
                <label>snu (서울대학교) - <a href=https://gitlab.com/qufa/qufa/-/tree/master/dataFairness/snu>source code</a></label>
            </li>
          </ul>
        </li><br>
        <li>
          <label>dataQuality (데이터 품질팀)</label>
          <ul>
            <li>
                <label>uos (서울시립대학교) - <a href=https://gitlab.com/qufa/qufa/-/tree/master/dataQuality/uos>source code</a></label>
            </li>
            <li>
                <label>ewu (이화여자대학교) - <a href=https://gitlab.com/qufa/qufa/-/tree/master/dataQuality/ewu>source code</a></label>
            </li>
            <li>
                <label>hsu (한성대학교) - <a href=https://gitlab.com/qufa/qufa/-/tree/master/dataQuality/hsu>source code</a></label>
            </li>
          </ul>
        </li><br>
        <li>
          <label>businessDevelopment (상용화팀/수요기업)</label>
          <ul>
            <li>
                <label>PromptTechnology ((주)프람트테크놀로지) - <a href=https://gitlab.com/qufa/qufa/-/tree/master/businessDevelopment/PromptTechnology>source code</a></label>
            </li>
            <li>
                <label>i-Soft ((주)아이소프트) - <a href=https://gitlab.com/qufa/qufa/-/tree/master/businessDevelopment/I-Soft>source code</a></label>
            </li>
          </ul>
        </li><br>
        <li>
          <label>integratedSystem</label>
          <ul>
            <li>
                <label>QuFa 통합 프레임워크의 submodule 등록 - <a href=https://gitlab.com/qufa/qufa/-/tree/master/integratedSystem>more</a></label>
            </li>            
          </ul>
        </li>
      </ul>
    </li>
</ul>

##
- Status of QuFa Repository (Last update: Oct. 14, 2021)

| Name | Description of key features  | Link |
| ------ | ------ | ------ |
| QuFa | **※ Official repo. of QuFa**<br> - 도메인별 데이터셋 및 데이터 품질/공정성/통합(QuFa Framework) 구현 | - |
| Qufa_dataHunter | - 원천 데이터를 프로젝트 DB내에 로드<br> - 원천 데이터의 Meta data reading | <a href=https://gitlab.com/qufa/qufa_datahunter target="_blank">Go</a> |
| QuFa_dataReviewerServer | - 피드백 지원도구(백엔드)<br> - REST API 활용<br> - 파일 데이터, Database 정보를 통해 데이터를 API로 자동 변환 | <a href=https://gitlab.com/qufa/qufa_datareviewerserver target="_blank">Go</a> |
| QuFa_dataReviewerClient | - 피드백 지원도구(프론트엔드)<br> - Designer-Server와 연동<br> - React Framework 기반의 Web Frontend 프로젝트 | <a href=https://gitlab.com/qufa/qufa_datareviewerclient target="_blank">Go</a> |
| QuFa_dataFeat | - 데이터 특징공학 툴<br> | <a href=https://github.com/oslab-ewha/qufafeat target="_blank">Go</a> |
| QuFa_dataCloud | - 환경 데이터셋 저장소 클라우드<br> | <a href=https://drive.google.com/drive/folders/15-z-NWTicGJeEkYsbKUXaoBcwZJT9IKC target="_blank">Go</a> |

##
<div align="center"><strong>by Team QuFa</strong></div>
