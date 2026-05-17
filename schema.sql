create table contact_info (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    email varchar(255),
    linkedin varchar(255),
    phone varchar(255),
    primary key (id)
) engine=InnoDB;

create table education_matches (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    candidate_degree varchar(255),
    match_status varchar(255),
    required_degree varchar(255),
    primary key (id)
) engine=InnoDB;

create table experience_alignments (
    match_percentage float(53),
    years_candidate float(53),
    years_required float(53),
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

create table experience_ranges (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    max_years varchar(255),
    min_years varchar(255),
    primary key (id)
) engine=InnoDB;

create table personal_info (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    email varchar(255),
    first_name varchar(255),
    last_name varchar(255),
    location varchar(255),
    phone varchar(255),
    primary key (id)
) engine=InnoDB;

create table users (
    is_enabled bit not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    email varchar(255) not null,
    first_name varchar(255) not null,
    last_name varchar(255) not null,
    password varchar(255) not null,
    username varchar(255) not null,
    rh_approval_status enum ('APPROVED','PENDING','REJECTED') not null,
    role enum ('ADMIN','CANDIDATE','HR') not null,
    primary key (id),
    constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email),
    constraint UKr43af9ap4edm43mmtq01oddj6 unique (username)
) engine=InnoDB;

create table match_scores (
    overall_score float(53),
    created_at datetime(6) not null,
    education_match_id bigint,
    experience_alignment_id bigint,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    recommendation varchar(4000),
    reasoning varchar(8000),
    primary key (id),
    constraint UKkf1d2uq0ey8k5mh07gpchh3ir unique (education_match_id),
    constraint UKhhe0i879nfe1jatj5wp1keluw unique (experience_alignment_id),
    constraint FKef75fcf9s5xysatn8qyigj0uy foreign key (education_match_id) references education_matches (id),
    constraint FKfctvne791icb8o37rr2u8wubc foreign key (experience_alignment_id) references experience_alignments (id)
) engine=InnoDB;

create table profile_data (
    contact_info_id bigint,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    personal_info_id bigint,
    updated_at datetime(6) not null,
    primary key (id),
    constraint UKjkok9g5jr1elmo62gfdrr0ix5 unique (contact_info_id),
    constraint UKnq9e3xy67oeekbe10gi7eej5u unique (personal_info_id),
    constraint FKaxsnk23te1frnithl0fsasast foreign key (contact_info_id) references contact_info (id),
    constraint FKhrhjt80b2r6e12h4ohg0s75c8 foreign key (personal_info_id) references personal_info (id)
) engine=InnoDB;

create table hr_persons (
    id bigint not null,
    primary key (id),
    constraint FKbxr1vr4jt34quawy50mnwowva foreign key (id) references users (id)
) engine=InnoDB;

create table job_offers (
    created_at datetime(6) not null,
    created_by_id bigint not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    is_deleted boolean default false not null,
    status varchar(32) not null,
    jd_request_id varchar(255),
    title varchar(255) not null,
    raw_text LONGTEXT not null,
    primary key (id),
    constraint FKcen1ku8hc8r0t1k85uy7x1u1j foreign key (created_by_id) references users (id)
) engine=InnoDB;

create table refresh_tokens (
    revoked bit not null,
    created_at datetime(6) not null,
    expires_at datetime(6) not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    user_id bigint not null,
    token varchar(1000) not null,
    primary key (id),
    constraint UKghpmfn23vmxfu3spu3lfg4r2d unique (token),
    constraint FK1lih5y2npsf8u5o3vhdb9y0os foreign key (user_id) references users (id)
) engine=InnoDB;

create table admins (
    id bigint not null,
    primary key (id),
    constraint FKanhsicqm3lc8ya77tr7r0je18 foreign key (id) references users (id)
) engine=InnoDB;

create table candidates (
    id bigint not null,
    primary key (id),
    constraint FKpwx8qcbu3swnypnelf5b8db9j foreign key (id) references users (id)
) engine=InnoDB;

create table verification_codes (
    used bit not null,
    created_at datetime(6) not null,
    expires_at datetime(6) not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    user_id bigint not null,
    code varchar(10) not null,
    primary key (id),
    constraint FKa4qo6nts1xd94owirq5evcpda foreign key (user_id) references users (id)
) engine=InnoDB;

create table password_reset_tokens (
    token varchar(6) not null,
    created_at datetime(6) not null,
    expiry_date datetime(6) not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    user_id bigint not null,
    primary key (id),
    constraint UKla2ts67g4oh2sreayswhox1i6 unique (user_id),
    constraint FKk3ndxg5xp6v7wd4gjyusp15gq foreign key (user_id) references users (id)
) engine=InnoDB;

create table missing_skills (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    match_score_id bigint not null,
    updated_at datetime(6) not null,
    importance varchar(255),
    skill_name varchar(255),
    primary key (id),
    constraint FK61xiw2424t2roaa0cnilbk339 foreign key (match_score_id) references match_scores (id)
) engine=InnoDB;

create table matched_skills (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    match_score_id bigint not null,
    updated_at datetime(6) not null,
    name varchar(255),
    primary key (id),
    constraint FK9xfvvrvqljj6k6h6vrd5o8hlk foreign key (match_score_id) references match_scores (id)
) engine=InnoDB;

create table languages (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    profile_data_id bigint not null,
    updated_at datetime(6) not null,
    language varchar(255),
    level varchar(255),
    primary key (id),
    constraint FKcwkkinyvfnid8iegontvmco8p foreign key (profile_data_id) references profile_data (id)
) engine=InnoDB;

create table hobbies (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    profile_data_id bigint not null,
    updated_at datetime(6) not null,
    name varchar(255),
    primary key (id),
    constraint FK75nuiexoqjds64uymxmqfsu7h foreign key (profile_data_id) references profile_data (id)
) engine=InnoDB;

create table skills (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    profile_data_id bigint not null,
    updated_at datetime(6) not null,
    category varchar(255),
    name varchar(255),
    primary key (id),
    constraint FKsfr5q951j8sprrh4pfjs8tq76 foreign key (profile_data_id) references profile_data (id)
) engine=InnoDB;

create table experiences (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    profile_data_id bigint not null,
    updated_at datetime(6) not null,
    description varchar(4000),
    company varchar(255),
    end_date varchar(255),
    start_date varchar(255),
    title varchar(255),
    primary key (id),
    constraint FKmoqnylu1ou6vbq1t39v6q055f foreign key (profile_data_id) references profile_data (id)
) engine=InnoDB;

create table normalized_skills (
    years_experience float(53),
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    profile_data_id bigint not null,
    updated_at datetime(6) not null,
    category varchar(255),
    normalized_name varchar(255),
    original_name varchar(255),
    proficiency_level varchar(255),
    primary key (id),
    constraint FKjno1655klar66b0v4b2jtubd8 foreign key (profile_data_id) references profile_data (id)
) engine=InnoDB;

create table educations (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    profile_data_id bigint not null,
    updated_at datetime(6) not null,
    degree varchar(255),
    end_date varchar(255),
    honors varchar(255),
    institution varchar(255),
    start_date varchar(255),
    primary key (id),
    constraint FKc17u05ncr9n1ujx3mbh0gr524 foreign key (profile_data_id) references profile_data (id)
) engine=InnoDB;

create table certificates (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    profile_data_id bigint not null,
    updated_at datetime(6) not null,
    date varchar(255),
    issuer varchar(255),
    name varchar(255),
    primary key (id),
    constraint FKn2mkupt9d4h690foy1gmbu6r9 foreign key (profile_data_id) references profile_data (id)
) engine=InnoDB;

create table structured_jds (
    created_at datetime(6) not null,
    experience_range_id bigint,
    id bigint not null auto_increment,
    job_offer_id bigint,
    updated_at datetime(6) not null,
    company_name varchar(255),
    employment_type varchar(255),
    title varchar(255),
    work_location varchar(255),
    primary key (id),
    constraint UKkcwbbpl2h2apwn1ol69ilw2iu unique (experience_range_id),
    constraint UKcut4sbag85vsx1q3a0av8ffao unique (job_offer_id),
    constraint FKkosr2nti4stisyxdyrwr5tk3o foreign key (experience_range_id) references experience_ranges (id),
    constraint FKnvfkoupr3p1fshvpfaae0097r foreign key (job_offer_id) references job_offers (id)
) engine=InnoDB;

create table cvs (
    candidate_id bigint not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    job_offer_id bigint not null,
    profile_data_id bigint,
    updated_at datetime(6) not null,
    upload_date datetime(6) not null,
    file_url varchar(255) not null,
    ocr_payload_json longtext,
    raw_text longtext,
    status enum ('EVALUATED','FAILED','OCR_DONE','SENT_FOR_EVALUATION','UPLOADED') not null,
    primary key (id),
    constraint UKl8t7tdwd38nnj8l7jgvubr2da unique (profile_data_id),
    constraint FKpiu4o0wcsluogg8mld4q1s53i foreign key (candidate_id) references candidates (id),
    constraint FKk66v7v4hxqs121xrhsdm78gb3 foreign key (job_offer_id) references job_offers (id),
    constraint FKsrl4ay7ti1ey50402enqgrfr9 foreign key (profile_data_id) references profile_data (id)
) engine=InnoDB;

create table required_skills (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    structured_jd_id bigint not null,
    updated_at datetime(6) not null,
    name varchar(255) not null,
    primary key (id),
    constraint FKlsmersv067wj418uf40cnujur foreign key (structured_jd_id) references structured_jds (id)
) engine=InnoDB;

create table preferred_skills (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    structured_jd_id bigint not null,
    updated_at datetime(6) not null,
    name varchar(255) not null,
    primary key (id),
    constraint FKc1tyycld5s9c5sm93ignrh65k foreign key (structured_jd_id) references structured_jds (id)
) engine=InnoDB;

create table qualifications (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    structured_jd_id bigint not null,
    updated_at datetime(6) not null,
    description varchar(2000) not null,
    primary key (id),
    constraint FKssxmvnu380fyb3uw8k71ok3ap foreign key (structured_jd_id) references structured_jds (id)
) engine=InnoDB;

create table responsibilities (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    structured_jd_id bigint not null,
    updated_at datetime(6) not null,
    description varchar(2000) not null,
    primary key (id),
    constraint FKnnywjf0l06803q2kga28tnhx3 foreign key (structured_jd_id) references structured_jds (id)
) engine=InnoDB;

create table candidate_evaluations (
    created_at datetime(6) not null,
    cv_id bigint,
    id bigint not null auto_increment,
    match_score_id bigint,
    structured_jd_id bigint,
    updated_at datetime(6) not null,
    status enum ('FAILED','SCORED','WAITING') not null,
    primary key (id),
    constraint UK9lgjiqodwke825flaq8ngnvvi unique (cv_id),
    constraint UK1rp3t7wrtfeyvw0oyibxa3qd unique (match_score_id),
    constraint FK4sh3ycy4v84viqnpfwefbis3v foreign key (cv_id) references cvs (id),
    constraint FKaj987bmtexhjrkkptr4771x5h foreign key (match_score_id) references match_scores (id),
    constraint FKg4fkv1jgu68phx90e1011mbyp foreign key (structured_jd_id) references structured_jds (id)
) engine=InnoDB;

create table technical_questions (
    bluff_indicator bit,
    candidate_evaluation_id bigint not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    expected_answer varchar(4000),
    question varchar(4000),
    difficulty varchar(255),
    skill_area varchar(255),
    primary key (id),
    constraint FKdbhvffyqp7900ojn0y67r7jul foreign key (candidate_evaluation_id) references candidate_evaluations (id)
) engine=InnoDB;

create table hr_questions (
    candidate_evaluation_id bigint not null,
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    evaluation_criteria varchar(4000),
    psychological_intent varchar(4000),
    question varchar(4000),
    primary key (id),
    constraint FK21up1bh3w2xd6yavnvnk7xyym foreign key (candidate_evaluation_id) references candidate_evaluations (id)
) engine=InnoDB;

create table follow_up_questions (
    created_at datetime(6) not null,
    id bigint not null auto_increment,
    technical_question_id bigint not null,
    updated_at datetime(6) not null,
    text varchar(4000),
    primary key (id),
    constraint FKkpwk8irnpt2p88dc93sa6opq6 foreign key (technical_question_id) references technical_questions (id)
) engine=InnoDB;

create table follow_up_probes (
    created_at datetime(6) not null,
    hr_question_id bigint not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    text varchar(4000),
    primary key (id),
    constraint FK55clr9r8nktnu5cvm2kacwvuk foreign key (hr_question_id) references hr_questions (id)
) engine=InnoDB;

create table red_flags (
    created_at datetime(6) not null,
    hr_question_id bigint not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    text varchar(4000),
    primary key (id),
    constraint FKhqi8h8gjmles1ljxrp6bg9ac2 foreign key (hr_question_id) references hr_questions (id)
) engine=InnoDB;

create table ideal_response_indicators (
    created_at datetime(6) not null,
    hr_question_id bigint not null,
    id bigint not null auto_increment,
    updated_at datetime(6) not null,
    text varchar(4000),
    primary key (id),
    constraint FKftnch65237j77u6sxsotr1igd foreign key (hr_question_id) references hr_questions (id)
) engine=InnoDB;
