import { EndpointError } from '@vaadin/flow-frontend';
import { showNotification } from '@vaadin/flow-frontend/a-notification';
import { CSSModule } from '@vaadin/flow-frontend/css-utils';
import { Binder, field } from '@vaadin/form';
import '@vaadin/vaadin-button/vaadin-button';
import '@vaadin/vaadin-combo-box/vaadin-combo-box';
import '@vaadin/vaadin-custom-field/vaadin-custom-field';
import '@vaadin/vaadin-date-picker/vaadin-date-picker';
import '@vaadin/vaadin-form-layout/vaadin-form-layout';
import '@vaadin/vaadin-item/vaadin-item';
import '@vaadin/vaadin-notification/vaadin-notification';
import '@vaadin/vaadin-ordered-layout/vaadin-horizontal-layout';
import '@vaadin/vaadin-radio-button/vaadin-radio-button';
import '@vaadin/vaadin-radio-button/vaadin-radio-group';
import '@vaadin/vaadin-text-field/vaadin-email-field';
import '@vaadin/vaadin-text-field/vaadin-number-field';
import '@vaadin/vaadin-text-field/vaadin-text-field';
import { customElement, html, LitElement, PropertyValues, query, unsafeCSS } from 'lit-element';
import PersonModel from '../../generated/com/github/mcollovati/vertxvaadin/fusion/data/entity/PersonModel';
import * as PersonEndpoint from '../../generated/PersonEndpoint';
import styles from './person-form-view.css';

@customElement('person-form-view')
export class PersonFormViewElement extends LitElement {
  @query('#countryCode')
  private countryCode: any;

  static get styles() {
    return [CSSModule('lumo-typography'), unsafeCSS(styles)];
  }

  private binder = new Binder(this, PersonModel);

  protected firstUpdated(_changedProperties: PropertyValues) {
    super.firstUpdated(_changedProperties);

    this.countryCode.items = ['+354', '+91', '+62', '+98', '+964', '+353', '+44', '+972', '+39', '+225'];
  }

  render() {
    return html`
      <h3>Personal information</h3>
      <vaadin-form-layout style="width: 100%;">
        <vaadin-text-field label="First name" ...="${field(this.binder.model.firstName)}"></vaadin-text-field>
        <vaadin-text-field label="Last name" ...="${field(this.binder.model.lastName)}"></vaadin-text-field>
        <vaadin-date-picker label="Birthday" ...="${field(this.binder.model.dateOfBirth)}"></vaadin-date-picker>
        <vaadin-custom-field label="Phone number" ...="${field(this.binder.model.phone)}">
          <vaadin-horizontal-layout theme="spacing">
            <vaadin-combo-box
              id="countryCode"
              style="width: 120px;"
              pattern="\\+\\d*"
              placeholder="Country"
              prevent-invalid-input
            ></vaadin-combo-box>
            <vaadin-text-field
              style="width: 120px; flex-grow: 1;"
              pattern="\\d*"
              prevent-invalid-input
            ></vaadin-text-field>
          </vaadin-horizontal-layout>
        </vaadin-custom-field>
        <vaadin-email-field label="Email address" ...="${field(this.binder.model.email)}"></vaadin-email-field>
        <vaadin-text-field label="Occupation" ...="${field(this.binder.model.occupation)}"></vaadin-text-field>
      </vaadin-form-layout>
      <vaadin-horizontal-layout class="button-layout" theme="spacing">
        <vaadin-button theme="primary" @click="${this.save}"> Save </vaadin-button>
        <vaadin-button @click="${this.clearForm}"> Cancel </vaadin-button>
      </vaadin-horizontal-layout>
    `;
  }

  private async save() {
    try {
      await this.binder.submitTo(PersonEndpoint.update);
      this.clearForm();
      showNotification('Person details stored.', { position: 'bottom-start' });
    } catch (error) {
      if (error instanceof EndpointError) {
        showNotification('Server error. ' + error.message, { position: 'bottom-start' });
      } else {
        throw error;
      }
    }
  }

  private clearForm() {
    this.binder.clear();
  }
}
