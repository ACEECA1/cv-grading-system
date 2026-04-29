package org.djezzy.pfe.model.evaluation;

import org.djezzy.pfe.model.auth.*;
import org.djezzy.pfe.model.job.*;
import org.djezzy.pfe.model.evaluation.*;
import org.djezzy.pfe.model.system.*;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "profile_data")
public class ProfileData extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "personal_info_id")
    private PersonalInfo personalInfo;

    @OneToMany(mappedBy = "profileData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Experience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "profileData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Education> education = new ArrayList<>();

    @OneToMany(mappedBy = "profileData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Skill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "profileData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Language> languages = new ArrayList<>();

    @OneToMany(mappedBy = "profileData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certificate> certificates = new ArrayList<>();

    @OneToMany(mappedBy = "profileData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Hobby> hobbies = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "contact_info_id")
    private ContactInfo contactInfo;

    @OneToMany(mappedBy = "profileData", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NormalizedSkill> normalizedSkills = new ArrayList<>();

    public void clearSkills() {
        skills.clear();
    }

    public void addSkill(Skill skill) {
        if (skill == null) {
            return;
        }
        skills.add(skill);
        skill.setProfileData(this);
    }

    public void clearHobbies() {
        hobbies.clear();
    }

    public void addHobby(Hobby hobby) {
        if (hobby == null) {
            return;
        }
        hobbies.add(hobby);
        hobby.setProfileData(this);
    }

    public void clearExperiences() {
        experiences.clear();
    }

    public void addExperience(Experience experience) {
        if (experience == null) {
            return;
        }
        experiences.add(experience);
        experience.setProfileData(this);
    }

    public void clearEducation() {
        education.clear();
    }

    public void addEducation(Education educationEntry) {
        if (educationEntry == null) {
            return;
        }
        education.add(educationEntry);
        educationEntry.setProfileData(this);
    }

    public void clearLanguages() {
        languages.clear();
    }

    public void addLanguage(Language language) {
        if (language == null) {
            return;
        }
        languages.add(language);
        language.setProfileData(this);
    }

    public void clearCertificates() {
        certificates.clear();
    }

    public void addCertificate(Certificate certificate) {
        if (certificate == null) {
            return;
        }
        certificates.add(certificate);
        certificate.setProfileData(this);
    }

    public void clearNormalizedSkills() {
        normalizedSkills.clear();
    }

    public void addNormalizedSkill(NormalizedSkill normalizedSkill) {
        if (normalizedSkill == null) {
            return;
        }
        normalizedSkills.add(normalizedSkill);
        normalizedSkill.setProfileData(this);
    }
}




