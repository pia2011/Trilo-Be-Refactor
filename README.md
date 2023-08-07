# TRILO

## ✈️ [Trilo](http://cosain-trilo.com/)와 함께 여행 계획을 세워보아요!

Trilo에서는 **손 쉽게 여행 계획**을 짜고, 여행하며 모바일로 **여행 계획을 바로바로 확인**할 수 있습니다!

#### 여행 별 모아보기
![image](https://github.com/pia2011/Today-I-Learned/assets/53935439/2f67d78e-b4d5-4ebd-b1c3-02f25741a4e0)
#### 여행 계획 작성하기
![image](https://github.com/pia2011/Today-I-Learned/assets/53935439/249bcb79-c93a-44d7-b310-a1b18787ad8b)
#### 상세 일정 작성하기
![image](https://github.com/pia2011/Today-I-Learned/assets/53935439/b7836765-ce8a-4a7c-b2cc-bc4554bb8177)

## 사용 기술
- 언어 : Java 17
- 프레임워크 : Spring Boot 3.0
- 데이터베이스 : MySQL 8
- 데이터베이스 접근 기술 : Spring Data JPA, Querydsl
- CI/CD : Github Actions
- API 문서화 : Spring Rest Docs

## 🔨 혼자서 해보는 리팩토링

<details>
    <summary> 본문 확인 (👈 Click) </summary>
    
  ## 1. 클래스와 메서드의 수는 최소로 줄여라

프로젝트 초기부터 가장 많은 의견 충돌이 발생했던 부분이 Spring MVC 패턴에서 Controller 와 Service 의 public method 를 
하나로 제한시켜야하는 지에 대한 논쟁이었습니다.

함께 프로젝트를 진행했던 팀원분은 귀찮더라도 클래스 내부 복잡도를 줄이고 Git 충돌을 최소화 시키려면 하나로 두는게 맞다는 의견이셨고,
저는 그렇게 하면 클래스 내부 복잡도는 줄어들지만, 프로젝트 전체 복잡도가 올라가고 공통적으로 쓰이는 메서드의 재활용 자체가 불가능해진다. 결국 중복된 
코드도 늘어날 것이고 점점 리팩토링 및 수정하기 힘들어질 것이다 라는 의견이었습니다.

정말 수 많은 의견 충돌을 겪었지만 결국 팀 프로젝트는 팀원분의 주장대로 Service 와 Controller 의 public method 를 하나로 두고 진행되었지만, 예상했던 대로 중복코드도 늘어나고
점점 프로젝트를 한눈에 파악하기 어려워졌습니다. 개발 생산성은 떨어졌고 기능 추가 및 수정, 리팩토링을 할 때마다 고역이었습니다.

결국 의견 프로젝트가 어느정도 마무리되고 제가 올린 [OKKY 커뮤니티의 질문글과 답변들](https://okky.kr/questions/1462230)을 참고하여 다시 리팩토링 하였습니다.

public method 를 하나로 두기보다는 각각의 의존성과 목적을 고려하여 Command 와 Query 로만 분리하였습니다. 

![image](https://github.com/pia2011/Trilo-Be-Refactor/assets/53935439/2c28736a-ee12-4f8a-918c-fd95cfda4225)

</details>

## 🚀 링크

- [GITHUB](https://github.com/teamCoSaIn/trilo-be)
- [API DOCS](http://api.cosain-trilo.com/docs/)
- [Trilo 소개 페이지](https://jthw.notion.site/Trilo-7c990cfbcefb40909c0e6eec34fd6218?pvs=4)



