package org.djezzy.pfe.model.evaluation;

import org.djezzy.pfe.model.auth.*;
import org.djezzy.pfe.model.job.*;
import org.djezzy.pfe.model.evaluation.*;
import org.djezzy.pfe.model.system.*;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "contact_info")
public class ContactInfo extends BaseEntity {
    @Column
    private String email;

    @Column
    private String phone;

    @Column
    private String linkedin;
}




