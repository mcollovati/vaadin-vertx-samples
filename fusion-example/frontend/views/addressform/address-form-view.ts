import { EndpointError } from '@vaadin/flow-frontend';
import { showNotification } from '@vaadin/flow-frontend/a-notification';
import { CSSModule } from '@vaadin/flow-frontend/css-utils';
import { Binder, field } from '@vaadin/form';
import '@vaadin/vaadin-button/vaadin-button';
import '@vaadin/vaadin-combo-box/vaadin-combo-box';
import '@vaadin/vaadin-form-layout/vaadin-form-layout';
import '@vaadin/vaadin-ordered-layout/vaadin-horizontal-layout';
import '@vaadin/vaadin-text-field/vaadin-number-field';
import '@vaadin/vaadin-text-field/vaadin-text-field';
import { customElement, html, LitElement, PropertyValues, query, unsafeCSS } from 'lit-element';
import * as AddressEndpoint from '../../generated/AddressEndpoint';
import AddressModel from '../../generated/com/github/mcollovati/vertxvaadin/fusion/data/entity/AddressModel';
import styles from './address-form-view.css';

@customElement('address-form-view')
export class AddressFormView extends LitElement {
  @query('#state')
  private state: any;
  @query('#country')
  private country: any;

  static get styles() {
    return [CSSModule('lumo-typography'), unsafeCSS(styles)];
  }

  private binder = new Binder(this, AddressModel);

  protected firstUpdated(_changedProperties: PropertyValues) {
    super.firstUpdated(_changedProperties);

    this.state.items = ['State 1', 'State 2', 'State 3'];
    this.country.items = ['Country 1', 'Country 2', 'Country 3'];
  }

  render() {
    return html`
      <h3>Address</h3>
      <vaadin-form-layout>
        <vaadin-text-field
          colspan="2"
          label="Street address"
          ...="${field(this.binder.model.street)}"
        ></vaadin-text-field>
        <vaadin-number-field label="Postal code" ...="${field(this.binder.model.postalCode)}"></vaadin-number-field>
        <vaadin-text-field label="City" ...="${field(this.binder.model.city)}"></vaadin-text-field>
        <vaadin-combo-box id="state" label="State" ...="${field(this.binder.model.state)}"></vaadin-combo-box>
        <vaadin-combo-box id="country" label="Country" ...="${field(this.binder.model.country)}"></vaadin-combo-box>
      </vaadin-form-layout>
      <vaadin-horizontal-layout class="button-layout" theme="spacing">
        <vaadin-button theme="primary" @click="${this.save}"> Save </vaadin-button>
        <vaadin-button @click="${this.clearForm}">Cancel</vaadin-button>
      </vaadin-horizontal-layout>
    `;
  }

  private async save() {
    try {
      await this.binder.submitTo(AddressEndpoint.update);
      this.clearForm();
      showNotification('Address stored.', { position: 'bottom-start' });
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
